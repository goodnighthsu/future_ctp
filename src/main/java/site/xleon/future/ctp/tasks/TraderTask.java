package site.xleon.future.ctp.tasks;

import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.services.impl.TraderSpiImpl;
import ctp.thosttraderapi.CThostFtdcTraderApi;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Data
@Slf4j
@Component
public class TraderTask implements Runnable {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CThostFtdcTraderApi traderApi;

    @Autowired
    private TraderSpiImpl traderSpi;

    public void run()  {
        // 交易
        traderApi.RegisterSpi(traderSpi);
        traderApi.RegisterFront(appConfig.getTraderAddress());
        traderApi.SubscribePublicTopic(ctp.thosttraderapi.THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.SubscribePrivateTopic(ctp.thosttraderapi.THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.Init();
        log.info("trader task start");
        traderApi.Join();
    }
}
