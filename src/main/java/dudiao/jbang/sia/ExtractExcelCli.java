package dudiao.jbang.sia;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.CharsetDetector;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvWriter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import dudiao.jbang.sia.event.DealPostEvent;
import dudiao.jbang.utils.CsvUtils;
import dudiao.jbang.utils.PatternMatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.event.EventBus;
import picocli.CommandLine;

import java.io.File;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author songyinyin
 * @since 2022/11/12 21:22
 */
@Slf4j
@Component
@CommandLine.Command(name = "extract", description = "解析sia来源Excel")
public class ExtractExcelCli implements NbootCliService {

    @Inject
    private ExcelProperties excelProperties;
    @Inject
    private AviatorEvaluatorInstance instance;

    private List<Charset> charsets = Arrays.asList(CharsetUtil.CHARSET_UTF_8, CharsetUtil.CHARSET_GBK);

    @CommandLine.Option(names = {"-e", "--excel"}, paramLabel = "<file>", defaultValue = "/Users/songyinyin/xiaoyu/test",
        description = "Excel路径，默认为：/Users/songyinyin/xiaoyu/test")
    private File excelFile;

    @Override
    public Integer call() throws Exception {
        if (excelFile == null) {
            log.error("Excel路径为空");
            return 2;
        }


        if (!excelFile.isDirectory()) {
            log.error("Excel路径[{}]不是一个文件夹", excelFile.getAbsoluteFile());
            return 3;
        }
        if (excelFile.listFiles() == null || excelFile.listFiles().length == 0) {
            log.error("Excel路径[{}]下没有文件", excelFile.getAbsoluteFile());
            return 4;
        }
        Map<String, ExcelProperties.ExcelInfo> sourceConfig = excelProperties.getConfig();
        if (CollUtil.isEmpty(sourceConfig)) {
            log.error("请配置分类信息！");
            return 5;
        }

        String exportFilePath = excelProperties.getExportPath() + String.format("/result_%s.csv", DateUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_PATTERN));
        String currentExportFilePath = excelProperties.getExportPath() + "/result.csv";
        CsvWriter writer = CsvUtil.getWriter(exportFilePath, CharsetUtil.CHARSET_UTF_8);
        writer.writeHeaderLine(excelProperties.getTemplateHeader().toArray(new String[0]));

        // 将其他文件转为csv
        fileToCsv(sourceConfig);

        List<Map<String, String>> total = new ArrayList<>();
        /**
         * 每个渠道，处理后的明细行
         */
        Map<String, List<Map<String, String>>> dealMap = new LinkedHashMap<>();

        StopWatch stopWatch = new StopWatch("处理csv");
        for (File file : excelFile.listFiles()) {
            for (Map.Entry<String, ExcelProperties.ExcelInfo> entry : sourceConfig.entrySet()) {
                String key = entry.getKey();
                ExcelProperties.ExcelInfo excelInfo = entry.getValue();

                if (match(excelInfo, file)) {
                    List<Map<String, String>> deal;
                    try {
                        stopWatch.start(key);
                        deal = deal(writer, file, key, excelInfo);
                        if (CollUtil.isEmpty(deal)) {
                            deal = readOtherCharset(writer, total, file, key, excelInfo);
                        } else {
                            total.addAll(deal);
                        }

                    } catch (Exception e) {
                        log.warn("处理出错：", e);
                        deal = readOtherCharset(writer, total, file, key, excelInfo);
                    } finally {
                        stopWatch.stop();
                    }
                    dealMap.put(key, deal);
                    break;
                }
            }
        }
        writer.flush();

        FileUtil.copy(new File(exportFilePath), new File(currentExportFilePath), true);
        log.info("共处理{}条数据. \n{}", total.size(), stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
        log.info("生成的Excel路径：{}", exportFilePath);
        log.info("生成的Excel路径（当前）：{}", currentExportFilePath);
        groupByAndSum(total);

        EventBus.publish(new DealPostEvent(dealMap, sourceConfig));
        return 0;
    }

    /**
     * 使用其他编码读取文件
     */
    private List<Map<String, String>> readOtherCharset(CsvWriter writer, List<Map<String, String>> total, File file, String key, ExcelProperties.ExcelInfo excelInfo) {
        for (Charset charset : charsets) {
            log.warn("尝试使用其他编码读取：{}", charset);
            excelInfo.setFileCharset(charset);
            List<Map<String, String>> dealTwo = deal(writer, file, key, excelInfo);
            if (CollUtil.isNotEmpty(dealTwo)) {
                total.addAll(dealTwo);
                return dealTwo;
            }
        }
        return new ArrayList<>();
    }

    private void groupByAndSum(List<Map<String, String>> total) {
        StopWatch stopWatch = new StopWatch("统计");
        stopWatch.start("分组求和");
        Function<Map<String, String>, Map<String, String>> groupByKey = e -> {
            Map<String, String> map = new LinkedHashMap<>();
            for (String header : excelProperties.getHeaderGroupBy()) {
                map.put(header, e.get(header));
            }
            return map;
        };
        Map<Map<String, String>, Map<String, Double>> collect = total.stream()
            .collect(Collectors.groupingBy(groupByKey, LinkedHashMap::new, Collectors.collectingAndThen(Collectors.toList(), list -> {
                Map<String, Double> result = new LinkedHashMap<>();
                for (String sumHeader : excelProperties.getHeaderSum()) {
                    double sum = list.stream().mapToDouble(item -> {
                        String value = item.get(sumHeader);
                        if (StrUtil.isBlank(value)) {
                            return 0;
                        }
                        try {
                            value = value.replaceAll(",", "");
                            return Double.parseDouble(value);
                        } catch (Exception e) {
                            log.warn("求和出错，value={}, map={}", value, item);
                            return 0;
                        }
                    }).sum();
                    result.put(sumHeader, sum);
                }
                return result;
            })));
        stopWatch.stop();

        stopWatch.start("写入文件");
        String sumExportFilePath = excelProperties.getExportPath() + "/result_sum.csv";
        CsvWriter writer = CsvUtil.getWriter(sumExportFilePath, CharsetUtil.CHARSET_UTF_8);
        List<String> headers = new ArrayList<>();
        headers.addAll(excelProperties.getHeaderGroupBy());
        headers.addAll(excelProperties.getHeaderSum());
        writer.writeHeaderLine(headers.toArray(new String[0]));

        for (Map.Entry<Map<String, String>, Map<String, Double>> entry : collect.entrySet()) {
            List<Object> line = new ArrayList<>();
            line.addAll(entry.getKey().values());
            line.addAll(entry.getValue().values());
            writer.writeLine(Convert.toStrArray(line));
        }
        writer.flush();
        stopWatch.stop();
        log.info("生成统计文件：{}\n{}", sumExportFilePath, stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
    }

    /**
     * 将其他文件转为csv
     */
    private void fileToCsv(Map<String, ExcelProperties.ExcelInfo> sourceConfig) {
        for (File file : excelFile.listFiles()) {
            String suffix = FileUtil.getSuffix(file.getName());
            if (!StrUtil.equals("csv", suffix)) {

                for (Map.Entry<String, ExcelProperties.ExcelInfo> entry : sourceConfig.entrySet()) {
                    String key = entry.getKey();
                    ExcelProperties.ExcelInfo excelInfo = entry.getValue();
                    if (matchName(excelInfo, file)) {
                        log.info("将文件[{}] 转为csv", file.getName());
                        ExcelReader reader = ExcelUtil.getReader(file, excelInfo.getSheetIndex());
                        List<Map<String, Object>> read = reader.readAll();

                        String filePath = String.format("%s/%s.csv", excelFile.getAbsoluteFile(), FileUtil.mainName(file));
                        CsvWriter csvWriter = CsvUtil.getWriter(filePath, StandardCharsets.UTF_8);
                        csvWriter.writeLine(read.get(0).keySet().toArray(new String[0]));
                        List<Collection<Object>> collect = read.stream().map(Map::values).collect(Collectors.toList());
                        csvWriter.write(collect);

                        csvWriter.flush();
                        reader.close();
                    }
                }
            }
        }
    }

    /**
     * 只匹配文件名，不匹配后缀
     */
    private boolean matchName(ExcelProperties.ExcelInfo excelInfo, File file) {
        String[] split = excelInfo.getNamePattern().split(",");
        return PatternMatchUtils.simpleMatch(split, FileUtil.mainName(file).toLowerCase() + ".csv");
    }

    private boolean match(ExcelProperties.ExcelInfo excelInfo, File file) {
        String[] split = excelInfo.getNamePattern().split(",");
        return PatternMatchUtils.simpleMatch(split, file.getName().toLowerCase());
    }

    private List<Map<String, String>> deal(CsvWriter writer, File file, String key, ExcelProperties.ExcelInfo value) {
        log.info("开始处理：【{}】，文件名：{}", key, file.getName());
        // 文件编码，不指定时，自动探测文件编码
        Charset fileCharset = value.getFileCharset() != null ? value.getFileCharset() : CharsetDetector.detect(file);

        char fieldSeparator = CsvUtils.guessFieldSeparator(file, fileCharset);
        CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig();
        csvReadConfig
            .setBeginLineNo(value.getBeginLineNo())
            .setHeaderLineNo(value.getHeaderLineNo())
            .setFieldSeparator(fieldSeparator)
            .setTrimField(true);
        CsvReader reader = CsvUtil.getReader(csvReadConfig);

        CsvData read = reader.read(file, fileCharset);
        List<Map<String, String>> sortList = new ArrayList<>();
        for (int i = value.getDataStartLineNo() - 1; i < read.getRows().size(); i++) {
            CsvRow row = read.getRows().get(i);
            Map<String, String> item = getNullMap(excelProperties.getTemplateHeader(), key);

            Map<String, String> fieldMapping = value.getFieldMapping();
            for (Map.Entry<String, String> mappingEntry : fieldMapping.entrySet()) {
                String templateFieldName = mappingEntry.getKey();
                String sourceFieldName = mappingEntry.getValue();
                String expression = null;
                int index;
                if ((index = sourceFieldName.indexOf(",")) >= 0) {
                    expression = sourceFieldName.substring(index + 1);
                    sourceFieldName = sourceFieldName.substring(0, index);
                }
                if (templateFieldName.startsWith("fix:")) {
                    item.put(templateFieldName.substring(4), sourceFieldName);
                } else {
                    String fieldValue = null;
                    if (StrUtil.isNotBlank(sourceFieldName)) {
                        String rowByName = row.getByName(sourceFieldName);
                        // 指定了来源列名，但是row中获取不到值，说明是 乱码，直接跳过
                        if (StrUtil.isEmpty(rowByName)) {
                            continue;
                        }
                        fieldValue = rowByName;
                    }

                    if (StrUtil.isNotBlank(expression)) {

                        Map<String, Object> param = new HashMap<>();
                        param.put("value", fieldValue);
                        param.put("fileName", FileUtil.mainName(file.getName()));
                        try {
                            fieldValue = (String) instance.execute(expression, param, true);
                        } catch (Exception e) {
                            log.warn("执行表达式出错，param={}", param);
                        }
                    }
                    item.put(templateFieldName, fieldValue);
                }
            }
            // 忽略 展现，点击，消费都是0的数据
            if (isIgnore(item)) {
                continue;
            }
            // 处理公共字段
            for (Map.Entry<String, String> entry : excelProperties.getCommonField().entrySet()) {
                Map<String, Object> param = new HashMap<>();
                param.put("properties", BeanUtil.beanToMap(excelProperties));
                param.put("item", item);
                item.put(entry.getKey(), (String) instance.execute(entry.getValue(), param, true));
            }
            writer.writeLine(Convert.toStrArray(item.values()));
            sortList.add(item);
        }
        log.info("处理完成：【{}】，共 {} 条数据", key, sortList.size());
        if (sortList.size() == 0) {
            log.warn("可能是文件读取乱码，读取文件的表头为：{}", StrUtil.sub(StrUtil.toString(read.getHeader()), 0, 20));
            log.warn("当前的文件编码为：{}", fileCharset);
        }
        return sortList;
    }

    /**
     * 展现，点击，消费都是0的数据，需要忽略
     *
     * @param item 当前的数据行
     */
    private boolean isIgnore(Map<String, String> item) {
        return isNull(item.get("展现")) && isNull(item.get("消费")) && isNull(item.get("点击"));
    }

    private boolean isNull(String value) {
        if (StrUtil.isBlank(value)) {
            return true;
        }
        try {
            BigDecimal num = new BigDecimal(value.trim());
            return num.doubleValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, String> getNullMap(List<String> templateHeader, String source) {
        Map<String, String> item = new LinkedHashMap<>();
        for (String key : templateHeader) {
            if (key.equals("渠道")) {
                item.put(key, source);
            } else {
                item.put(key, null);
            }
        }
        return item;
    }
}
