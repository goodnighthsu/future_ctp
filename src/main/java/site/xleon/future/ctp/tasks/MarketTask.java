package site.xleon.future.ctp.tasks;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import site.xleon.future.ctp.config.app_config.AppConfig;
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
    private final AppConfig appConfig;
    private final MdService mdService;
    private final TradeService tradeService;

    @Override
    public void run() {
        Thread.currentThread().setName("MarketTask");
        new Thread(this::monitorConnect).start();
        new Thread(this::monitorLogin).start();
    }

    /**
     * 行情自动订阅
     */
    public void instrumentScribe() {
        List<InstrumentEntity> instrumentEntities = tradeService.listTradings();
        List<String> instruments = instrumentEntities.stream()
                .map(InstrumentEntity::getInstrumentID)
                .collect(Collectors.toList());
        log.info("行情订阅所有交易合约: {} 条", instruments.size());
        mdService.subscribe(instruments);
    }

    /**
     * 监控行情前置连接状态变更
     * 行情前置
     * 没有断线 -> 等待状态更新
     * 断线 -> 自动重连 -> 6s后重新查看状态
     */
    public void monitorConnect() {
        log.info("行情前置 监控连接状态");
        while (true) {
            synchronized (MdService.connectLock) {
                log.info("行情前置 连接状态: {} ", MdService.getConnectState());
                if (StateEnum.DISCONNECT != MdService.getConnectState()){
                    // 行情前置没有断线，监控连接状态
                    try {
                        MdService.connectLock.wait();
                        log.info("行情前置 连接状态更新: {} ", MdService.getConnectState());
                    } catch (Exception e) {
                        log.error("error: ", e);
                    }
                }
            }
            // 行情前置断线6s后重连
            if (StateEnum.DISCONNECT == MdService.getConnectState()) {
                try {
                    log.info("行情前置 自动重连");
                    MdService.connectFronts(appConfig.getMarketFronts());
                } catch (Exception e) {
                    log.error("行情前置 连接: {}", e.getMessage());
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
     * 监控行情前置登录状态变更
     * 行情前置
     * 登录 -> 更新合约
     * 没有登录 -> 等待状态更新
     * 断线 -> 自动重连 -> 6s后重新查看状态
     */
    public void monitorLogin() {
        log.info("行情前置 监控登录状态");
        while (true) {
            // 监控交易登录状态
            synchronized (MdService.loginLock) {
                log.info("行情前置 登录状态: {} ", MdService.getLoginState());
                if (StateEnum.SUCCESS == MdService.getLoginState()) {
                    // 登录成功
                    instrumentScribe();
                }

                if ( StateEnum.DISCONNECT != MdService.getLoginState() ) {
                    // 没有登录
                    try {
                        MdService.loginLock.wait();
                    }   catch (Exception e) {
                        log.error("error: ", e);
                    }
                }
            }

            if (StateEnum.DISCONNECT == MdService.getLoginState()) {
                // 6s后重新登录
                try {
                    log.info("行情前置 自动重新登录");
                    mdService.login(appConfig.getUser());
                } catch (Exception e) {
                    log.error("行情前置 登录: {}", e.getMessage());
                }

                try {
                    Thread.sleep(6000);
                }catch (Exception e) {
                    log.error("行情前置 登录: {}", e.getMessage());
                }
            }
        }
    }
}
