package site.xleon.future.ctp.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Configuration
@Slf4j
public class MainTask {

    @Autowired
    private MarketTask marketTask;

    @Autowired
    private TraderTask traderTask;

    /**
     * 行情服务
     */
    @Autowired
    private MarketService marketService;

    /**
     * 交易服务
     */
    @Autowired
    private TradeService tradeService;

    @Autowired
    private DataService dataService;

    @Bean
    public ExecutorService executorService() {
        ThreadFactory threadFactory = new CustomizableThreadFactory("mainPool-");
        ExecutorService executor = new ThreadPoolExecutor(
                3,
                3,
                0L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                threadFactory
        );

        Thread autScribeThread = new Thread(this::autoScribe);

        executor.execute(marketTask);
        executor.execute(traderTask);
        executor.execute(autScribeThread);

        return executor;
    }

    /**
     * 自动订阅
     */
    public void autoScribe() {
        while (true) {
            // 订阅登录状态变更
            synchronized (CtpInfo.loginLock) {
                try {
                    log.info("订阅登录状态");
                    CtpInfo.loginLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // simnow交易服务经常不成功，使用本地订阅文件订阅所有行情
                try {
                    if (!marketService.getIsLogin()) {
                        log.warn("自动订阅失败: 行情服务未登录");
                        continue;
                    }
                    List<String> subscribes = dataService.readSubscribe();
                    marketService.subscribe(subscribes);
                }catch (Exception e) {
                    log.error("自动订阅失败: {}", e.getMessage());
                }

                /*
                try {
                    if (!marketService.getIsLogin()) {
                        log.warn("自动订阅失败: 行情服务未登录");
                        continue;
                    }
                    if (!tradeService.getIsLogin()) {
                        log.warn("自动订阅失败: 交易服务未登录");
                        continue;
                    }

                    log.info("自动订阅开始");
                    // 获取合约
                    // 交易日可能切换，清除合约缓存
                    tradeService.clearInstruments();
                    List<InstrumentEntity> all = tradeService.instruments(null);
                    log.info("自动订阅合约获取成功: {}", all.size());
                    // 订阅合约
                    marketService.subscribe(all.stream().map(InstrumentEntity::getInstrumentID).collect(Collectors.toList()));
                } catch (Exception e) {
                    log.error("自动订阅失败: {}", e.getMessage());
                }
                 */
            }
        }
    }
}
