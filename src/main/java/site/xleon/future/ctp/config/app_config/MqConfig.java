package site.xleon.future.ctp.config.app_config;

import lombok.Data;

@Data
public class MqConfig {
    private String host;
    private String virtualHost;
    private String username;
    private String password;
}
