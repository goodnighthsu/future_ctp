package site.xleon.future.ctp.tasks;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.services.impl.MqService;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class MainTask {
    private final MarketTask marketTask;
    private final TradeTask tradeTask;
    private final MqService mqService;

    @Bean
    private void init() throws InterruptedException, IOException, TimeoutException {
        CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
        threadFactory.setThreadNamePrefix("Task");
        Executor executor = Executors.newFixedThreadPool(2,
                threadFactory
        );
        // trade task
        executor.execute(tradeTask);
        Thread.sleep(6000);
        // market task
        executor.execute(marketTask);
        // mq task
        mqService.create();
    }
}
