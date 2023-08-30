package dudiao.jbang.sia.event;

import cn.hutool.core.collection.CollUtil;
import dudiao.jbang.sia.ExcelProperties;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.annotation.Component;
import org.noear.solon.core.event.EventListener;

import java.util.List;
import java.util.Map;

/**
 * @author songyinyin
 * @since 2023/1/31 15:33
 */
@Slf4j
@Component
public class SiaListener implements EventListener<DealPostEvent> {

    @Override
    public void onEvent(DealPostEvent dealPostEvent) throws Throwable {
        Map<String, List<Map<String, String>>> dealMap = dealPostEvent.getDealMap();
        for (Map.Entry<String, ExcelProperties.ExcelInfo> entry : dealPostEvent.getConfig().entrySet()) {
            String key = entry.getKey();
            List<Map<String, String>> resultList = dealMap.get(key);
            if (CollUtil.size(resultList) < 10) {
                log.warn("渠道【{}】获取到的数据为 {} 条，请检查！文件匹配名为：{}", key, CollUtil.size(resultList), dealPostEvent.getConfig().get(key).getNamePattern());
            }
        }
    }
}
