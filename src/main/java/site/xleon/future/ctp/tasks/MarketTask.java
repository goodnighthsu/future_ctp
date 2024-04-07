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

@Data
@Component
@Slf4j
public class MarketTask implements Runnable {
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private MdService mdService;

    @Autowired
    TradeService tradeService;

    @Override
    public void run() {
        Thread.currentThread().setName("MarketTask");
        autoScribe();
    }

    /**
     * 行情自动订阅
     */
    public void autoScribe() {
        try {
            log.info("配置行情前置 {}", appConfig.getMarketFronts());
            MdService.setFronts(appConfig.getMarketFronts());
            log.info("行情自动登录: {}", appConfig.getUser().getUserId());
            mdService.login(appConfig.getUser());
            // 登录成功
            List<InstrumentEntity> instrumentEntities = tradeService.listTradings();
            List<String> instruments = instrumentEntities.stream()
                    .map(InstrumentEntity::getInstrumentID)
                    .collect(Collectors.toList());
            log.info("订阅所有交易合约: {}条", instruments.size());
            mdService.subscribe(instruments);
        } catch (Exception e) {
            log.error("行情订阅失败: ", e);
        } finally {
            // 断线或退出登录，等待3秒后自动重新发起
            monitor();
        }
    }

    public void monitor() {
        // 监控退出登录
        synchronized (MdService.loginLock) {
            while (StateEnum.SUCCESS == MdService.getConnectState()) {
                try {
                    log.info("监控行情登录状态");
                    MdService.loginLock.wait();
                } catch (InterruptedException e) {
                    log.error("interrupt: ",e);
                }

                if (StateEnum.SUCCESS != MdService.getLoginState()) {
                    break;
                }
            }
        }

        log.info("行情用户退出登录, 6s后尝试重新开始更新合约任务");
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            log.error("interrupt: ", e);
        }
        autoScribe();
    }
}
