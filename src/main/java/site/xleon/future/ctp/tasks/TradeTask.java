package site.xleon.future.ctp.tasks;

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
        init();
        autoScribe();
    }

    /**
     * 配置init
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
     * 交易自动登录，更新所有合约信息
     */
    public void autoScribe() {
        try {
            log.info("配置交易前置 {}", appConfig.getTraderFronts());
            TradeService.setFronts(appConfig.getTraderFronts());
            log.info("交易自动登录: {}", appConfig.getUser().getUserId());
            tradeService.login(appConfig.getUser());
            // 登录成功
            log.info("更新所有合约信息:");
            List<InstrumentEntity> instrumentEntities = tradeService.listInstruments(null);
            tradeService.updateInstrument(instrumentEntities);
        } catch (Exception e) {
            log.error("合约订阅失败: ", e);
        } finally {
            // 断线或退出登录，等待3秒后自动重新发起
            monitorLogin();
        }
    }

    public void monitorLogin() {
        // 监控退出登录
        synchronized (TradeService.loginLock) {
            while (StateEnum.SUCCESS == TradeService.getLoginState()) {
                try {
                    log.info("监控交易登录状态");
                    TradeService.loginLock.wait();
                } catch (InterruptedException e) {
                    log.error("interrupt: ", e);
                }
            }
        }

        log.info("交易用户退出登录,6s后尝试重新开始更新合约任务");
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            log.error("interrupt: ", e);
        }
        //
        autoScribe();
    }
}
