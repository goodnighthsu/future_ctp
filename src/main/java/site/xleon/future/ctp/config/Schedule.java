package site.xleon.future.ctp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.CtpMasterClient;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

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
     * 交易自动登录
     */
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
     * 行情自动登录
     */
    @Async
    @Scheduled(cron = "0 55 8,20 * * MON-FRI")
    public void autoMarketLogin() {
        try {
            log.info("行情自动登录");
            marketService.login();
            log.info("行情自动登录成功, 交易日: {}", ctpInfo.getTradingDay());
        } catch (Exception e) {
            log.error("行情自动登录失败: ", e);
            try {
                Thread.sleep(6000);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            autoMarketLogin();
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