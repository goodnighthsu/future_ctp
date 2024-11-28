package site.xleon.future.ctp.tasks;
;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.TradeService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

/**
 * 创建交易日history、flow目录
 * 监控交易前置登录状态
 * 没有登录 -> 自动登录
 * 已登录 -> 更新合约信息
 */
@Data
@Component
@Slf4j
public class TradeTask implements Runnable {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private TradeService tradeService;

    public void run() {
        Thread.currentThread().setName("TradeTask");
        // 创建交易日history、flow目录
        init();
        // 登录交易前置
        connectAndLogin();
        // 监控登录状态
        monitorLogin();
    }

    /**
     * 配置init
     * 创建交易日目录history
     */
    private void init() {
        if (!appConfig.getHistoryPath().toFile().exists()) {
            boolean isSuccess = appConfig.getHistoryPath().toFile().mkdirs();
            if(!isSuccess) {
                log.error("init: create history directory failure");
                SpringApplication.exit(context, () -> 0);
            }

            isSuccess = Paths.get("flow").toFile().mkdirs();
            if(!isSuccess) {
                log.error("init: create flow directory failure");
                SpringApplication.exit(context, () -> 0);
            }
        }
    }

    /**
     * 更新合约信息
     */
    public void updateInstruments() {
        log.info("更新所有合约信息:");
        List<InstrumentEntity> instrumentEntities = tradeService.listInstruments(null);
        tradeService.updateInstrument(instrumentEntities);
    }

    /**
     * 连接交易前置并登录
     */
    public void connectAndLogin() {
        try {
            log.info("交易前置 {} 连接", appConfig.getTraderFronts());
            TradeService.connectFronts(appConfig.getTraderFronts());
        } catch (Exception e) {
            log.warn("交易前置 {} 连接失败", appConfig.getMarketFronts());
            return;
        }

        try {
            log.info("交易前置，用户 {} 登录 ", appConfig.getUser().getUserId());
            tradeService.login(appConfig.getUser());
        } catch (Exception e) {
            log.info("交易前置，用户 {} 登录失败", appConfig.getUser().getUserId());
        }
    }

    /**
     * 监控交易前置登录状态变更
     * 交易前置
     * 没有登录 -> 自动登录
     * 已登录 -> 更新合约信息
     */
    public void monitorLogin() {
        // 监控交易登录状态
        synchronized (TradeService.loginLock) {
            log.info("交易前置 监控登录状态");
            while (true) {
                log.info("交易前置 登录状态变更: {} ", TradeService.getLoginState());
                try {
                    if (TradeService.getLoginState() != StateEnum.SUCCESS) {
                        // 没有登录
                        Thread.sleep(12000);
                        new Thread(this::connectAndLogin).start();
                    } else {
                        // 登录成功
                        updateInstruments();
                    }
                    TradeService.loginLock.wait();
                } catch (Exception e) {
                    log.error("error: ", e);
                }
            }
        }
    }
}
