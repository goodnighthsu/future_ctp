package site.xleon.future.ctp.tasks;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import site.xleon.future.ctp.config.app_config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.MdService;
import site.xleon.future.ctp.services.impl.TradeService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控行情前置登录状态
 * 没有登录 -> 自动登录
 * 已登录 -> 订阅行情
 */
@Data
@Component
@Slf4j
public class MarketTask implements Runnable {
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private MdService mdService;

    @Autowired
    private TradeService tradeService;

    @Override
    public void run() {
        Thread.currentThread().setName("MarketTask");
        connectAndLogin();
        monitor();
    }

    /**
     * 行情自动订阅
     */
    public void instrumentScribe() {
        List<InstrumentEntity> instrumentEntities = tradeService.listInstruments(null);
        List<String> instruments = instrumentEntities.stream()
                .map(InstrumentEntity::getInstrumentID)
                .collect(Collectors.toList());
        log.info("行情订阅所有交易合约: {} 条", instruments.size());
        mdService.subscribe(instruments);
    }

    /**
     * 连接交易前置并登录
     */
    public void connectAndLogin() {
        try {
            log.info("行情前置 {} 连接", appConfig.getMarketFronts());
            MdService.connectFronts(appConfig.getMarketFronts());
        } catch (Exception e) {
            log.warn("行情前置 {} 连接失败", appConfig.getMarketFronts());
            return;
        }

        try {
            log.info("行情前置，用户 {} 登录: ", appConfig.getUser().getUserId());
            mdService.login(appConfig.getUser());
        } catch (Exception e) {
            log.info("行情前置，用户 {} 登录失败", appConfig.getUser().getUserId());
        }
    }

    public void monitor() {
        synchronized (MdService.loginLock) {
            log.info("行情前置 监控登录状态");
            while (true) {
                log.info("行情前置 登录状态变更: {} ", MdService.getLoginState());
                try {
                    if (MdService.getLoginState() != StateEnum.SUCCESS) {
                        // 没有登录
                        Thread.sleep(12000);
                        new Thread(this::connectAndLogin).start();
                    }
                    if(MdService.getLoginState() == StateEnum.SUCCESS) {
                        // 行情交易前置和交易前置都登录成功, 订阅合约
                        Thread.sleep(3000);
                        instrumentScribe();
                    }
                    MdService.loginLock.wait();
                } catch (Exception e) {
                    log.error("error: ", e);
                }
            }
        }
    }
}
