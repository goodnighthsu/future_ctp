package site.xleon.future.ctp.tasks;

import site.xleon.future.ctp.config.app_config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

import java.util.concurrent.*;

@Configuration
public class MainTask {

    @Autowired
    private MarketTask marketTask;

    @Autowired
    private TraderTask traderTask;

    @Autowired
    private SubscribeTask subscribeTask;

    @Autowired
    private AppConfig appConfig;

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

        executor.execute(marketTask);
        executor.execute(traderTask);
//        executor.execute(subscribeTask);

        return executor;
    }
}
