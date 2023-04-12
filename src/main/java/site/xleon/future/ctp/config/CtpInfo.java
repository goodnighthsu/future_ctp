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

@EnableAsync
@EnableScheduling
@Data
@Slf4j
@Component
public class CtpInfo {
    /**
     * 市场前置是否连接
     */
    private boolean isMarketFrontConnected = false;

    /**
     * 是否登录
     */
    private boolean isMarketLogin = false;

    /**
     * 交易前置是否连接
     */
    private boolean isTradingFrontConnected = false;

    private String tradingDay;
    @SneakyThrows
    public String getTradingDay() {
        if (!isMarketFrontConnected) {
            throw new MyException("行情前置未连接");
        }
        if (tradingDay == null) {
            tradingDay = mdApi.GetTradingDay();
        }
        return tradingDay;
    }


    @Autowired
    private CThostFtdcMdApi mdApi;

    @Scheduled(fixedRate = 1000 * 60)
    public void updateTradingDay() {
        setTradingDay(mdApi.GetTradingDay());
        log.warn("update trading day: {}", tradingDay);
    }
}
