package dudiao.jbang.sia;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvRowHandler;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author songyinyin
 * @since 2023/8/12 12:03
 */
@Slf4j
public class KeywordRowHandler implements CsvRowHandler {

  private final ExcelProperties excelProperties;

  private final AviatorEvaluatorInstance instance;

  private final CsvWriter matchedWriter;

  @Getter
  private final String matchedFile;

  private final CsvWriter excludeWriter;

  @Getter
  private final String excludeFile;

  private int total = 0;

  @Getter
  private int excludeNum = 0;

  @Getter
  private List<Map<String, String>> list = new ArrayList<>();

  public KeywordRowHandler(String filePath, ExcelProperties excelProperties, AviatorEvaluatorInstance instance) {
    String timeStr = DateUtil.format(DateUtil.date(), "yyyyMMdd_HHmmss");
    this.matchedFile = String.format("%s/result_%s.csv", filePath, timeStr);
    CsvWriter matchedWriter = CsvUtil.getWriter(matchedFile, CharsetUtil.CHARSET_UTF_8);
    List<String> headers = new ArrayList<>(excelProperties.getKeywordGroup().keySet());
    headers.add(0, "关键词");
    headers.add("得分");
    matchedWriter.writeHeaderLine(headers.toArray(new String[0]));
    this.matchedWriter = matchedWriter;

    this.excludeFile = String.format("%s/exclude_%s.csv", filePath, timeStr);
    CsvWriter excludeWriter = CsvUtil.getWriter(excludeFile, CharsetUtil.CHARSET_UTF_8);
    excludeWriter.writeHeaderLine("关键词");
    this.excludeWriter = excludeWriter;

    this.excelProperties = excelProperties;
    this.instance = instance;
  }

  @Override
  public void handle(CsvRow row) {
    Map<String, String> data = row.getFieldMap();

    list.add(data);
    total++;

    if (list.size() >= 10000) {
      matchKeyword(list);
      list.clear();
      log.info("已经处理：{}", total);
    }
  }

  public int dealLast() {
    if (CollUtil.isNotEmpty(list)) {
      matchKeyword(list);
      list.clear();
    }
    return total;
  }

  private void matchKeyword(List<Map<String, String>> dataList) {

    Set<String> keywords = dataList.stream()
      .filter(e -> {
        String key = e.get("关键词");
        return StrUtil.isNotBlank(key);
      })
      .map(e -> e.get("关键词"))
      .collect(Collectors.toCollection(LinkedHashSet::new));

    forKey:
    for (String keyword : keywords) {
      String keywordLowerCase = keyword.toLowerCase();
      for (String exclude : excelProperties.getKeywordExclude()) {
        if (keywordLowerCase.contains(exclude)) {
          this.excludeNum++;
          excludeWriter.writeLine(keyword);
          continue forKey;
        }
      }

      Map<String, Object> map = new LinkedHashMap<>();
      map.put("关键词", keyword);

      for (Map.Entry<String, Map<String, List<String>>> entry : excelProperties.getKeywordGroup().entrySet()) {
        String groupName = entry.getKey();
        map.put(groupName, null);
        Map<String, List<String>> group = entry.getValue();
        aa:
        for (Map.Entry<String, List<String>> runEntry : group.entrySet()) {
          String key = runEntry.getKey();
          List<String> values = runEntry.getValue();
          for (String value : values) {

            if (keywordLowerCase.contains(value)) {
              map.put(groupName, key);
              break aa;
            }
          }
        }
      }
      int score = 0;
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (StrUtil.equals(key, "关键词")) {
          continue;
        }
        if (value == null) {
          continue;
        }
        score = score + 10;
      }
      Map<String, Object> param = new HashMap<>();
      param.put("r", map);
      for (String expression : excelProperties.getScore()) {
        Object execute = instance.execute(expression, param, true);
        if (execute instanceof Number) {
          score = ((Number) execute).intValue() + score;
        }
      }
      map.put("得分", score);
      matchedWriter.writeLine(Convert.toStrArray(map.values()));

    }

    matchedWriter.flush();
    excludeWriter.flush();

  }
}
