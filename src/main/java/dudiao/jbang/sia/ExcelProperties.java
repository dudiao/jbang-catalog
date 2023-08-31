package dudiao.jbang.sia;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author songyinyin
 * @since 2022/11/13 10:56
 */
@Data
public class ExcelProperties {

    /**
     * 模版文件的标题
     */
    private List<String> templateHeader = new ArrayList<>();

    /**
     * 文件导出的路径，不包括文件名
     */
    private String exportPath;

    /**
     * 每种来源文件的配置，key=来源
     */
    private Map<String, ExcelInfo> config = new LinkedHashMap<>();

    /**
     * 分类，按照渠道分类到组别，key=渠道 value=组别
     */
    private Map<String, String> groupBy = new LinkedHashMap<>();

    /**
     * 返点，key=渠道 value=返点
     */
    private Map<String, Double> rebate = new LinkedHashMap<>();

    /**
     * 中心判定，10大校区所在地（北京，上海，广州，深圳，武汉，成都，重庆，天津，南京，杭州）为中心，其余为周边
     */
    private List<String> centerCity = new ArrayList<>();

    /**
     * 公共字段
     */
    private Map<String, String> commonField = new LinkedHashMap<>();

    /**
     * key=校区 value=市
     */
    private Map<String, List<String>> citySchool = new LinkedHashMap<>();

    /**
     * key=校区 value=省
     */
    private Map<String, List<String>> provinceSchool = new LinkedHashMap<>();

    /**
     * 音乐校区划分
     */
    private Map<String, List<String>> musicSchool = new LinkedHashMap<>();

    /**
     * 按照表头的字段分组
     */
    private List<String> headerGroupBy = new LinkedList<>();

    /**
     * 分组后的求和字段
     */
    private List<String> headerSum = new LinkedList<>();

    /**
     * 关键词分组，key=组别 value=关键词
     */
    private Map<String, Map<String, List<String>>> keywordGroup = new LinkedHashMap<>();

    /**
     * 得分的计算公式
     */
    private List<String> score = new ArrayList<>();

    /**
     * 排除的关键词
     */
    private List<String> keywordExclude = new ArrayList<>();

    /**
     * 处理配置文件的数据，去除空格，去除重复
     */
    public void deal() {
        this.keywordExclude = keywordExclude.stream()
            .filter(StrUtil::isNotBlank)
            .map(e -> StrUtil.trim(e).toLowerCase())
            .distinct()
            .collect(Collectors.toList());

        keywordGroup.replaceAll((k, v) -> v.entrySet().stream()
            .filter(e -> StrUtil.isNotBlank(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList()))));
    }

    @Data
    public static class ExcelInfo {

        /**
         * 多个以英文的逗号(,)分隔
         */
        private String namePattern;

        /**
         * 将excel转为csv时，使用的sheet页
         */
        private Integer sheetIndex = 0;

        /**
         * 定义开始的行（包括），此处为原始文件行号
         */
        private Integer beginLineNo = 0;

        /**
         * 表头所在行
         */
        private Integer headerLineNo = 0;

        /**
         * 数据开始的行
         */
        private Integer dataStartLineNo = 1;

        /**
         * 文件编码，不指定时，自动探测文件编码
         */
        private Charset fileCharset;

        /**
         * 字段与模版的映射，key=该来源的字段名，value=模版字段名
         */
        private Map<String, String> fieldMapping = new LinkedHashMap<>();


        /**
         * 统一转小写
         */
        public String getNamePattern() {
            return namePattern.toLowerCase();
        }
    }

    @Data
    public static class GroupBy {

    }
}
