package tech.muyi.tspl.boot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * my-tspl Spring Boot 自动配置：注册 {@link TsplPrinterManager}，打印机地址由业务调用时传入。
 */
@Configuration
public class TsplAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public TsplPrinterManager tsplPrinterManager() {
        return new TsplPrinterManager();
    }
}
