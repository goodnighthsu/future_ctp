package site.xleon.future.ctp.tasks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.InstrumentEntity;
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

        Thread autScribeThread = new Thread(() -> {
            try {
                autoScribe();
            } catch (InterruptedException e) {
                log.error("自动订阅失败: {}", e.getMessage());
            }
        });

        executor.execute(marketTask);
        executor.execute(traderTask);
        executor.execute(autScribeThread);

        return executor;
    }

    /**
     * 自动订阅
     */
    public void autoScribe() throws InterruptedException {
        try {
            Thread.sleep(1000);
            // login
            marketService.login();
            tradeService.login();

            // 获取合约
            List<InstrumentEntity> all = tradeService.instruments(null);
            log.info("自动订阅合约获取成功: {}", all.size());

            log.info("自动订阅开始");
            marketService.subscribe(all.stream().map(InstrumentEntity::getInstrumentID).collect(Collectors.toList()));

        } catch (Exception e) {
            log.error("自动订阅失败: {}", e.getMessage());
            autoScribe();
        }

        //  订阅登录状态变更
        synchronized (MarketService.loginLock) {
            MarketService.loginLock.wait();
            log.info("自动订阅收到登录成功信号");
            autoScribe();
        }
    }
}
