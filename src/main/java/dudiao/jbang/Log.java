package dudiao.jbang;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;

/**
 * @author songyinyin
 * @since 2023/8/30 17:37
 */
public class Log {

    public static void info(String message, Object... params) {
        System.out.println(getHeader("INFO") + StrUtil.format(message, params));
    }

    public static void warn(String message, Object... params) {
        System.out.println(getHeader("WARN") + StrUtil.format(message, params));
    }

    public static void error(String message, Object... params) {
        System.err.println(getHeader("ERROR") + StrUtil.format(message, params));
    }

    private static String getHeader(String level) {
        return "[nboot] %s - %s : ".formatted(DateUtil.formatLocalDateTime(LocalDateTime.now()), level);
    }
}
