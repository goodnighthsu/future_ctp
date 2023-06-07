package site.xleon.future.ctp.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

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

                // sim now交易服务经常不成功，使用本地订阅文件订阅所有行情
                try {
                    if (tradeService.getIsLogin()) {
                        // 交易登录成功， 更新合约信息库
                        tradeService.updateInstrument();
                    }

                    if (!marketService.getIsLogin()) {
                        log.warn("自动订阅失败: 行情服务未登录");
                        continue;
                    }
                    List<String> subscribes = tradeService.listTrading().stream()
                            .map(InstrumentEntity::getInstrumentID).collect(Collectors.toList());
                    marketService.subscribe(subscribes);
                } catch (Exception e) {
                    log.error("自动订阅失败: {}", e.getMessage());
                }
            }
        }
    }
}
