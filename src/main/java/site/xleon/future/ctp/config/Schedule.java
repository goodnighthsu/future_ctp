package site.xleon.future.ctp.config;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import site.xleon.future.ctp.Result;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.CtpMasterClient;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;

@Configuration
@EnableAsync
@Slf4j
public class Schedule {
    @Autowired
    private TradeService tradeService;
    @Autowired
    private MarketService marketService;
    @Autowired
    private DataService dataService;

    @Autowired
    private CtpMasterClient marketClient;

    @Autowired
    private CtpInfo ctpInfo;

    /**
     * 自动登录
     */
    @Async
    @Scheduled(cron = "0 50 8,20 * * MON-FRI")
    public void autoLogin() {
        try {
            log.info("行情自动登录");
            // simnow 交易登录经常失败，先跳过
//            tradeService.login();
            marketService.login();
            log.info("行情自动登录成功, 交易日: {}", ctpInfo.getTradingDay());
        } catch (Exception e) {
            log.error("行情自动登录失败: ", e);
            try {
                Thread.sleep(6000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            autoLogin();
        }
    }

    @Async
    @Scheduled(cron = "0 50 8,20 * * MON-FRI")
    public void autoTradeLogin() {
        try {
            log.info("交易自动登录");
            String userId = tradeService.login();
            log.info("交易自动登录成功, 用户: {}", userId);
        } catch (Exception e) {
            log.error("交易自动登录失败: {}", e.getMessage());
            try {
                Thread.sleep(60 * 1000 * 5);
        } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            autoTradeLogin();
        }
    }

    /**
     * 自动压缩
     */
    @Async
    @Scheduled(cron = "0 0 6 * * ?")
    public void autoCompress () {
        log.info("自动压缩");
        dataService.compress();
        log.info("自动压缩完成");
    }

    @Async
    @Scheduled(cron = "0 0 5 * * ?")
    public void autoDownload () throws MyException {
       marketService.download();
    }
}