package dudiao.jbang.sia;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvUtil;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import dudiao.jbang.utils.PatternMatchUtils;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.annotation.Inject;
import picocli.CommandLine;

import java.io.File;
import java.io.FileReader;
import java.util.concurrent.TimeUnit;

/**
 * @author songyinyin
 * @since 2023/8/5 19:11
 */
@Slf4j
@Component
@CommandLine.Command(name = "keywordMatch", description = "关键词")
public class KeywordMatchCli implements NbootCliService {

    @CommandLine.Option(names = {"-e", "--excel"}, paramLabel = "<file>", defaultValue = "/Users/songyinyin/xiaoyu/keyword",
        description = "Excel路径，默认为：/Users/songyinyin/xiaoyu/keyword")
    private File excelFile;

    @Inject
    private ExcelProperties excelProperties;
    @Inject
    private AviatorEvaluatorInstance instance;

    @Override
    public Integer call() throws Exception {
        excelProperties.deal();

        for (File file : excelFile.listFiles()) {
            if (file.isDirectory()) {
                continue;
            }
            if (PatternMatchUtils.simpleMatch("关键词*.csv", file.getName())) {
                log.info("开始匹配：{}", file.getPath());

                StopWatch stopWatch = new StopWatch("关键词匹配");
                stopWatch.start("文件: " + file.getName());

                KeywordRowHandler keywordRowHandler = new KeywordRowHandler(file.getParentFile().getPath(), excelProperties, instance);
                CsvReadConfig csvReadConfig = CsvReadConfig.defaultConfig()
                    .setHeaderLineNo(0)
                    .setTrimField(true);
                CsvReader reader = CsvUtil.getReader(new FileReader(file), csvReadConfig);
                reader.read(keywordRowHandler);
                int total = keywordRowHandler.dealLast();

                stopWatch.stop();
                log.info("共匹配 {} 条关键词，其中排除 {} 条关键词。\n结果文件：{}\n排除文件：{}\n{}", total, keywordRowHandler.getExcludeNum(),
                    keywordRowHandler.getMatchedFile(), keywordRowHandler.getExcludeFile(), stopWatch.prettyPrint(TimeUnit.MILLISECONDS));
            }
        }
        return null;
    }


}
