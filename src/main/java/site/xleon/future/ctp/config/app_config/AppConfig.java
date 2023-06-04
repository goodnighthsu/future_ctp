package site.xleon.future.ctp.config.app_config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import java.util.Set;

@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "config")
public class AppConfig {
    private MySqlConfig mysql;
    private UserConfig user;
    private String traderAddress;
    private String marketAddress;
    private String exchange;

    /**
     * websocket token
     */
    private String token;

    /**
     *  合约
     */
    private Set<String> instruments;


}
