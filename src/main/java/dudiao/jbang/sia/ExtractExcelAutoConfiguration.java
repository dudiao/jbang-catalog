package dudiao.jbang.sia;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import lombok.SneakyThrows;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * @author songyinyin
 * @since 2022/11/13 10:59
 */
@Configuration
public class ExtractExcelAutoConfiguration {

  @Bean
  @SneakyThrows
  public AviatorEvaluatorInstance cliAviator() {
    AviatorEvaluatorInstance instance = AviatorEvaluator.getInstance();
    instance.useLRUExpressionCache(1000);
    instance.addStaticFunctions("sia", SiaUtils.class);
    instance.addInstanceFunctions("s", String.class);
    return instance;
  }

    @Bean
    public ExcelProperties excelProperties(@Inject("${sia}") ExcelProperties excelProperties) {
        return excelProperties;
    }
}
