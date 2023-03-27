package site.xleon.future.ctp.config.app_config;

import lombok.Data;

@Data
public class MySqlConfig {
    private String url;
    private String database;
    private String username;
    private String password;
}
