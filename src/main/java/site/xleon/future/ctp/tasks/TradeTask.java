package site.xleon.future.ctp.tasks;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.TradeService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
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
    private final ApplicationContext context;
    private final AppConfig appConfig;
    private final TradeService tradeService;

    public void run() {
        Thread.currentThread().setName("TradeTask");
        // 创建交易日history、flow目录
        init();
        // 监控交易连接状态
        new Thread(this::monitorConnect).start();
        // 监控登录状态
        new Thread(this::monitorLogin).start();
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
     * 监控交易前置连接状态变更
     * 交易前置
     * 没有断线 -> 等待状态更新
     * 断线 -> 自动重连 -> 6s后重新查看状态
     */
    public void monitorConnect() {
        // 监控交易登录状态
        log.info("交易前置 监控连接状态");
        while (true) {
            synchronized (TradeService.connectLock) {
                log.info("交易前置 连接状态: {} ", TradeService.getConnectState());
                if (StateEnum.DISCONNECT != TradeService.getConnectState()){
                    // 交易前置没有断线，监控连接状态
                    try {
                        TradeService.connectLock.wait();
                        log.info("交易前置 连接状态更新: {} ", TradeService.getConnectState());
                    } catch (Exception e) {
                        log.error("error: ", e);
                    }
                }
            }

            // 交易前置断线6s后重连
            if (StateEnum.DISCONNECT == TradeService.getConnectState()) {
                try {
                    log.info("交易前置 自动重连");
                    TradeService.connectFronts(appConfig.getTraderFronts());
                } catch (Exception e) {
                    log.error("交易前置 连接: {}", e.getMessage());
                }

                try {
                    Thread.sleep(6000);
                }catch (Exception e) {
                    log.error("交易前置 登录: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 监控交易前置登录状态变更
     * 交易前置
     * 登录 -> 更新合约
     * 没有登录 -> 等待状态更新
     * 断线 -> 自动重连 -> 6s后重新查看状态
     */
    public void monitorLogin() {
        log.info("交易前置 监控登录状态");
        while (true) {
            // 监控交易登录状态
            synchronized (TradeService.loginLock) {
                log.info("交易前置 登录状态: {} ", TradeService.getLoginState());
                if (StateEnum.SUCCESS == TradeService.getLoginState()) {
                    // 登录成功
                    updateInstruments();
                }

                if ( StateEnum.DISCONNECT != TradeService.getLoginState() ) {
                    // 没有登录
                    try {
                        TradeService.loginLock.wait();
                    }   catch (Exception e) {
                        log.error("error: ", e);
                    }
                }
            }

            if (StateEnum.DISCONNECT == TradeService.getLoginState()) {
                // 6s后重新登录
                try {
                    log.info("交易前置 自动重新登录");
                    tradeService.login(appConfig.getUser());
                } catch (Exception e) {
                    log.error("交易前置 登录: {}", e.getMessage());
                }

                try {
                    Thread.sleep(6000);
                }catch (Exception e) {
                    log.error("交易前置 登录: {}", e.getMessage());
                }
            }
        }
    }
}
