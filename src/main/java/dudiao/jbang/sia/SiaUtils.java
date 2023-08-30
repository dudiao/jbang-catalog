package dudiao.jbang.sia;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author songyinyin
 * @since 2022/11/15 18:49
 */
@Slf4j
public class SiaUtils {

    public static final String DATE_PATTERN = "yyyy/MM/dd";

    public static final String center_city = "中心";
    public static final String periphery_city = "周边";

    public static ExcelProperties properties = Solon.context().getBean(ExcelProperties.class);

    private static Map<String, String> musicSchoolReverse = new HashMap<>();

    static {
        Map<String, List<String>> musicSchool = properties.getMusicSchool();
        for (Map.Entry<String, List<String>> entry : musicSchool.entrySet()) {
            for (String school : entry.getValue()) {
                musicSchoolReverse.put(school, entry.getKey());
            }
        }
    }

    public static String formatDate(String source) {
        try {
            return formatDate0(source);
        } catch (Exception e) {
            log.warn("时间解析出错：{}", source);
            return source;
        }
    }

    /**
     * 音乐校区划分
     */
    public static String musicSchool(String school, String businessCategory) {
        if (businessCategory.contains("音乐") && musicSchoolReverse.containsKey(school)) {
            return musicSchoolReverse.get(school);
        }
        return school;
    }

    /**
     * 除法计算：value1/value2
     *
     * @return 保留两位小数，四舍五入
     */
    public static String mathDiv(Object value1, Object value2) {
        if (isNull(value1) || isNull(value2)) {
            return null;
        }
        BigDecimal val1 = new BigDecimal(value1.toString());
        BigDecimal val2 = new BigDecimal(value2.toString());
        return val1.divide(val2, 4, RoundingMode.HALF_UP).toString();
    }

    /**
     * 校区判定
     *
     * @param city     每一行数据的市
     * @param province 每一行数据的省
     */
    public static String school(String city, String province) {
        String school = getSchool(city, properties.getCitySchool());
        if (StrUtil.isNotBlank(school)) {
            return school;
        }

        // 市那一列，有可能是省的信息，所以按照省，再匹配一边
        school = getSchool(city, properties.getProvinceSchool());
        if (StrUtil.isNotBlank(school)) {
            return school;
        }

        school = getSchool(province, properties.getProvinceSchool());
        if (StrUtil.isNotBlank(school)) {
            return school;
        }

        school = getSchool(province, properties.getCitySchool());
        if (StrUtil.isNotBlank(school)) {
            return school;
        }
        return "深圳";
    }

    private static String getSchool(String target, Map<String, List<String>> schoolMap) {
        if (StrUtil.isBlank(target)) {
            return null;
        }
        for (Map.Entry<String, List<String>> entry : schoolMap.entrySet()) {
            for (String sourceCity : entry.getValue()) {
                if (target.contains(sourceCity)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }

    /**
     * 中心判定，属于十大城市的，返回：中心，其余的返回：周边
     */
    public static String isCenter(String target, List<String> centerCity) {
        if (StrUtil.isBlank(target)) {
            return null;
        }
        for (String city : centerCity) {
            if (target.contains(city)) {
                return center_city;
            }
        }
        return periphery_city;
    }

    /**
     * 业务类别，按照计划名称，含有音乐的，分为音乐，其余都是作品集
     */
    public static String businessCategory(String target) {
        if (StrUtil.isBlank(target)) {
            return "作品集";
        }
        if (target.contains("音乐")) {
            return "音乐";
        }
        return "作品集";
    }

    private static boolean isNull(Object value) {
        if (value == null) {
            return true;
        }
        return StrUtil.isBlank(value.toString());
    }

    public static String getMap(Map<String, Object> map, String key) {
        return map.get(key).toString();
    }


    /**
     * 微信搜一搜，市
     */
    public static String getWxSearchCity(String value) {
        String[] split = value.split("-");
        if (split.length == 2) {
            String city = split[1];
            // 当含有 未知 时，返回省
            if (city.contains("未知")) {
                return split[0];
            }
            return city;
        }
        return value;
    }

    /**
     * 优先取文件名上的日期（xxx-2022-10-11），取不到，则取 昨天
     */
    public static String getBusinessDate(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return yesterday();
        }
        String[] split = fileName.split("_");
        String date = split[split.length - 1];
        if (split.length == 1 || date.length() != 8) {
            return yesterday();
        }
        try {
            return formatDate0(date);
        } catch (Exception e) {
            return yesterday();
        }
    }

    /**
     * 昨天：2022/11/19
     */
    public static String yesterday() {
        return DateUtil.format(LocalDateTime.now().plusDays(-1), DATE_PATTERN);
    }


    /**
     * 将时间格式：yyyy-MM-dd yyyyMMdd yyyy/MM/dd，
     * 转化为字符串：yyyy/MM/dd
     */
    private static String formatDate0(String source) {
        // 支持匹配的格式：yyyy-MM-dd yyyyMMdd yyyy/MM/dd
        DateTime dateTime = DateUtil.parse(source, DatePattern.NORM_DATE_PATTERN, DATE_PATTERN, DatePattern.PURE_DATE_PATTERN);
        return DateUtil.format(dateTime, DATE_PATTERN);
    }

}
