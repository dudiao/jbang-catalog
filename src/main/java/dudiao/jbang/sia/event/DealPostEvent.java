package dudiao.jbang.sia.event;

import dudiao.jbang.sia.ExcelProperties;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * @author songyinyin
 * @since 2023/1/31 15:20
 */
@Getter
public class DealPostEvent {

  /**
   * 每种来源文件的配置，key=来源
   */
  private final Map<String, ExcelProperties.ExcelInfo> config;

  /**
   * 每个渠道，处理后的明细行
   */
  private final Map<String, List<Map<String, String>>> dealMap;

  public DealPostEvent(Map<String, List<Map<String, String>>> dealMap, Map<String, ExcelProperties.ExcelInfo> config) {
    this.dealMap = dealMap;
    this.config = config;
  }
}
