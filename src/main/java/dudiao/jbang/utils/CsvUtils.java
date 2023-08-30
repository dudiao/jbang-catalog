package dudiao.jbang.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;

/**
 * @author songyinyin
 * @since 2022/11/23 13:29
 */
public class CsvUtils {

  private static final char[] fieldSeparators = new char[]{',', '\t'};

  /**
   * 根据文件的前几行，猜测csv文件的 分隔符
   *
   * @param file
   * @param fileCharset
   * @return
   */
  @SneakyThrows
  public static char guessFieldSeparator(File file, Charset fileCharset) {
    BufferedReader reader = FileUtil.getReader(file, fileCharset);
    int rowNo = 0;
    StringBuilder multiLine = new StringBuilder();
    String line;
    while (rowNo < 5) {
      line = reader.readLine();
      if (line == null) {
        break;
      }
      rowNo++;
      multiLine.append(line);
    }

    int count = 0;
    char possibleFieldSeparator = ',';
    for (char fieldSeparator : fieldSeparators) {
      int tmp = StrUtil.count(multiLine.toString(), fieldSeparator);
      if (tmp > count) {
        count = tmp;
        possibleFieldSeparator = fieldSeparator;
      }
    }
    return possibleFieldSeparator;
  }
}
