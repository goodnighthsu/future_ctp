package site.xleon.future.ctp.config.app_config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Data
@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "config")
public class AppConfig {
    private MySqlConfig mysql;
    private UserConfig user;
    private List<String> traderFronts = new ArrayList<>();
    private List<String> marketFronts = new ArrayList<>();
    private ScheduleConfig schedule;

    /**
     * web socket token
     */
    private String token;

    /**
     *  ctp行情文件保存目录
     */
    public final Path historyPath = Paths.get("history");

    /**
     * ctp行情文件状态
     */
    public final Path historyDataPath = Paths.get("history", "data.json");

}
