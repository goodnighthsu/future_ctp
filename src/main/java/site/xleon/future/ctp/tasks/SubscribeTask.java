package site.xleon.future.ctp.tasks;

import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.services.impl.MdSpiImpl;
import site.xleon.future.ctp.services.impl.TraderSpiImpl;
import site.xleon.future.ctp.mapper.IInstrumentMapper;
import site.xleon.future.ctp.mapper.TradingSubscribeMapper;
import site.xleon.future.ctp.mapper.impl.InstrumentService;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.TradeService;
import ctp.thostmduserapi.CThostFtdcMdApi;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Data
@Component
public class SubscribeTask implements Runnable {
    @Autowired
    AppConfig appConfig;

    @Autowired
    private TradingSubscribeMapper tradingSubscribeMapper;

    @Autowired
    private CThostFtdcMdApi mdApi;

    @Autowired
    private MdSpiImpl mdSpi;

    @Autowired
    private TraderSpiImpl traderSpi;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private DataService dataService;

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private IInstrumentMapper instrumentMapper;

    @Autowired
    private CtpInfo ctpInfo;

    /**
     * 当前订阅的合约
     */
    private List<String> instruments = new ArrayList<>();

    /**
     * 订阅合约的更新时间
     */
    public Date updateTime;

    /**
     * 期货合约订阅开始时间
     */
    private String subscribeFuturesDate;


    /**
     * 自动开始订阅所有期货行情
     */
    private void autoSubscribeFutures() throws IOException {
        List<String> subscribed = dataService.readSubscribe();
        subscribe(subscribed);

//        // 当前已订阅
//        if (tradingDay.equals(getSubscribeFuturesDate()) && getSubscribeFuturesDate() != null) {
//            log.warn("subscribed skip: {}", tradingDay);
//            return;
//        }
//
//        // tradingDay变更或没有成功订阅， 开始订阅
//        List<InstrumentEntity> futures = tradingService.listAllFutures();
//
//        // 数据库内合约
//        List<InstrumentEntity> instruments = instrumentService.list();
//        List<InstrumentEntity> addFutures = futures.stream().filter( future -> {
//             return !instruments.stream().anyMatch(instrument -> {
//                 return future.getIsSubscribe().equals(instrument.getInstrumentID());
//            });
//        }).collect(Collectors.toList());
//
//        // 有新的期货合约， 添加的数据库
//        if (!addFutures.isEmpty()) {
//            List<InstrumentEntity> insert = new ArrayList<>();
//            addFutures.forEach(future -> {
//                future.setIsSubscribe(true);
//                insert.add(future);
//            });
//            instrumentService.saveBatch(insert);
//        }
//
//        // 需要订阅的
//        subscribeInstruments(appConfig.getExchange(), futures);
//        setSubscribeFuturesDate(tradingDay);
    }

    /**
     * ctp 订阅合约
     * @param instruments 合约id
     */
    private void subscribe(List<String> instruments) {
        // 创建订阅文件
        instruments.forEach(item -> {
            Path path = Paths.get("data", ctpInfo.getTradingDay(), item + "_"  + ctpInfo.getTradingDay() + ".csv");
            dataService.initFile(path);
        });

        // 订阅
        String[] ids = new String[instruments.size()];
        instruments.toArray(ids);
        mdApi.SubscribeMarketData(ids, ids.length);
        log.info("start subscribe {} instruments", ids.length);
    }

    private void subscribeInstruments(String exchange, List<InstrumentEntity> instruments) {
//        List<InstrumentEntity> lastInstruments;
//        if (exchange == null || exchange.equals("")) {
//            lastInstruments = instrumentService.list();
//        }else {
//            lastInstruments = instrumentService.listTradingInstrumentsByExchange(exchange);
//        }
//        List<String> subscribed = futures.stream()
//                .filter( future -> {
//                    InstrumentEntity find = lastInstruments.stream()
//                            .filter( instrument -> instrument.getInstrumentID().equals(future.getInstrumentID()))
//                            .findAny().orElse(null);
//                    if(find != null) {
//                        return find.getIsSubscribe();
//                    }
//
//                    return false;
//                })
//                .map(InstrumentEntity::getInstrumentID)
//                .collect(Collectors.toList());

        String[] ids = new String[instruments.size()];
        instruments.toArray(ids);
        mdApi.SubscribeMarketData(ids, ids.length);
        log.info("start subscribe {} instruments", ids.length);
    }

    @Override
    public void run() {

//        while (true) {
//            try {
//                Thread.sleep(6000);
//            }catch (InterruptedException exception) {
//                Thread.currentThread().interrupt();
//                continue;
//            }
//
//            try {
//                if (traderSpi.getIsLogin()) {
//                    autoSubscribeFutures();
//                    continue;
//                }
//                log.info("subscribe task skip not login");
//            } catch (Exception exception) {
//                log.error("error: ", exception);
//            }
//
//        }
    }
}
