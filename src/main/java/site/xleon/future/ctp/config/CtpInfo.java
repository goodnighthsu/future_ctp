package site.xleon.future.ctp.config;

import ctp.thostmduserapi.CThostFtdcMdApi;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;


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
}
