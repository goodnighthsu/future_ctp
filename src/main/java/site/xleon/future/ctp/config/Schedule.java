package site.xleon.future.ctp.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import site.xleon.future.ctp.core.utils.CompressUtils;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


@Configuration
@EnableAsync
@Slf4j
public class Schedule {
    @Autowired
    private TradeService tradeService;
    @Autowired
    private MarketService marketService;

    @Autowired
    private CtpInfo ctpInfo;

    /**
     * 自动登录
     */
    @Async
    @Scheduled(cron = "0 50 8,20 * * MON-FRI")
    public void autoLogin() {
        try {
            log.info("自动登录");
            tradeService.login();
            marketService.login();
            log.info("自动登录成功, 交易日: {}", ctpInfo.getTradingDay());
        } catch (Exception e) {
            log.error("自动登录失败: ", e);
        }
    }

    /**
     * 自动压缩
     */
    @Async
    @Scheduled(cron = "0 0 7 * * ?")
    public void autoCompress () {
        log.info("auto compress start");
        Path path = Paths.get("data");
        File[] files = path.toFile().listFiles();
        if (files == null) {
            log.info("auto compress skip, no files");
            return;
        }
        Arrays.stream(files)
                .forEach(item -> {
                    // 是目录且不是当天交易日目录
                    if (item.isDirectory() && !item.getName().equals(ctpInfo.getTradingDay())) {
                        log.info("auto compress dir {}", item.getName());
                        try {
                            log.info("{} compress start", item.getName());
                            CompressUtils.tar(item.toPath(), Paths.get("data", item.getName() + ".tar.gz"));
                            log.info("{} compress start", item.getName());
                            FileUtils.deleteDirectory(item);
                            log.info("{} deleted", item.getName());
                        } catch (IOException e) {
                            log.error("{} compress error: ", item.getName(), e);
                            throw new RuntimeException(e);
                        }
                    }
                });
        log.info("auto compress finish");
    }
}
