package site.xleon.future.ctp.tasks;

import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.core.MdSpiImpl;
import ctp.thostmduserapi.CThostFtdcMdApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketTask implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(MarketTask.class);

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CThostFtdcMdApi mdApi;

    @Autowired
    private MdSpiImpl mdSpi;

    @Override
    public void run() {
        // 行情登录 market
        mdApi.RegisterSpi(mdSpi);
        mdApi.RegisterFront(appConfig.getMarketAddress());
        mdApi.Init();
        logger.info("market task start");
        mdApi.Join();
    }
}
