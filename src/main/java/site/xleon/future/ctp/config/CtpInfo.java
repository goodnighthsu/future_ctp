package site.xleon.future.ctp.config;

import ctp.thostmduserapi.CThostFtdcMdApi;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

import java.util.List;
import java.util.stream.Collectors;

@EnableAsync
@EnableScheduling
@Data
@Slf4j
@Component
public class CtpInfo {
    /**
     * 交易日
     */
    private String tradingDay;
    @SneakyThrows
    public String getTradingDay() {
        if (tradingDay == null) {
            tradingDay = mdApi.GetTradingDay();
        }
        return tradingDay;
    }

    /**
     * 登录状态通知
     */
    public static final Object loginLock = new Object();


    @Autowired
    private CThostFtdcMdApi mdApi;

    @Scheduled(fixedRate = 1000 * 60)
    public void updateTradingDay() {
        setTradingDay(mdApi.GetTradingDay());
        log.warn("update trading day: {}", tradingDay);
    }

}
