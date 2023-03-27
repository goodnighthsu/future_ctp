package site.xleon.future.ctp.config;

import ctp.thostmduserapi.CThostFtdcMdApi;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.core.MdSpiImpl;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.DataService;

import java.util.List;

@EnableAsync
@EnableScheduling
@Data
@Slf4j
@Component
public class CtpInfo {
    @Autowired
    private DataService dataService;

    private String tradingDay;

    /**
     * 订阅的合约
     */
    private List<String> subscribeInstruments;
    public List<String> getSubscribeInstruments() {
        if (subscribeInstruments == null) {
            subscribeInstruments = dataService.readSubscribe();
        }
        return subscribeInstruments;
    }

    public void setSubscribeInstruments(List<String> subscribeInstruments) {
        this.subscribeInstruments = subscribeInstruments;
        dataService.saveSubscribe(subscribeInstruments);
    }

    /**
     * 当前交易日合约
     */
    private List<InstrumentEntity> instruments;
    public List<InstrumentEntity> getInstruments(String tradingDay) {
        if (tradingDay == null ) {
            // 从本地文件读取
            if (instruments == null) {
                instruments = dataService.readInstrumentsTradingDay(this.tradingDay);
            }

            // 从缓存中读取
            return instruments;
        }

        // 从本地文件中读取
        return dataService.readInstrumentsTradingDay(this.tradingDay);
    }

    @Autowired
    private CThostFtdcMdApi mdApi;

    @Scheduled(fixedRate = 1000 * 60)
    public void updateTradingDay() {
        setTradingDay(mdApi.GetTradingDay());
        log.warn("update trading day: {}", tradingDay);
    }
}
