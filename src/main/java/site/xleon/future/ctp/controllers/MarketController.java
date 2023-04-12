package site.xleon.future.ctp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import site.xleon.future.ctp.Result;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private CtpInfo ctpInfo;

    @Autowired
    private MarketService marketService;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private DataService dataService;

    /**
     * ctp 登录
     * @return trading day
     */
    @GetMapping("/login")
    public Result<String> login() {
        String tradingDay = marketService.login();
        return Result.success(tradingDay);
    }


    @GetMapping("/logout")
    public Result<String> logout() {
        String result = marketService.logout();
        return Result.success(result);
    }

    @GetMapping("/tradingDay")
    public Result<String> tradingDay() {
        String tradingDay = ctpInfo.getTradingDay();
        return Result.success(tradingDay);
    }

    /**
     * 订阅合约
     *
     * @param params 合约id
     * @return 订阅的合约
     */
    @PutMapping("/subscribe")
    public Result<List<String>> subscribe(@RequestBody List<String> params) {
        List<String> subscribes = marketService.getSubscribeInstruments();
        subscribes.addAll(params);
        marketService.subscribe(subscribes);
        marketService.setSubscribeInstruments(subscribes);
        return Result.success(params);
    }

    /**
     * 取消订阅
     *
     * @param params 合约id
     * @return 取消订阅的合约
     */
    @PutMapping("/unsubscribe")
    public Result<List<String>> unsubscribe(@RequestBody List<String> params) {
        marketService.unsubscribe(params);
        // ctpInfo 移除订阅信息
        List<String> subscribes = marketService.getSubscribeInstruments();
        subscribes.removeAll(params);
        marketService.setSubscribeInstruments(subscribes);
        return Result.success(params);
    }

    /**
     * 获取指定合约的行情
     */
    @GetMapping("/market")
    public Result<List<String>> market(
            @RequestParam @NonNull String id,
            @RequestParam @Nullable String tradingDay,
            @RequestParam(defaultValue = "0") Integer index
    ) {
        if (tradingDay == null || tradingDay.isEmpty()) {
            tradingDay = ctpInfo.getTradingDay();
        }
        return Result.success(dataService.readMarket(tradingDay, id, index));
    }

    /**
     * 订阅全市场合约
     */
    @GetMapping("/subscribeAll")
    public Result<String> subscribeAll() {
        List<InstrumentEntity> all = tradeService.instruments(null);
//        all = all.stream().limit(1000).collect(Collectors.toList());
//        InstrumentEntity entity = new InstrumentEntity();
//        entity.setInstrumentID("fu2309");
//        all.add(entity);
        marketService.subscribe(all.stream().map(InstrumentEntity::getInstrumentID).collect(Collectors.toList()));
//        List list = new ArrayList();
//        list.add("fu2305");
//        marketService.subscribe(list);
        return Result.success("ok");
    }
}
