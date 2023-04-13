package site.xleon.future.ctp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import site.xleon.future.ctp.Result;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;

import java.lang.reflect.InvocationTargetException;
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
    public Result<String> login() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        String tradingDay = marketService.login();
        return Result.success(tradingDay);
    }

    @GetMapping("/logout")
    public Result<String> logout() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
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
     * @param instrument 合约id
     * @param tradingDay 交易日
     * @return 行情
     */
    @GetMapping("/query")
    public Result<List<String>> query(
            @RequestParam @NonNull String instrument,
            @RequestParam @Nullable String tradingDay,
            @RequestParam(defaultValue = "0") Integer index
    ) {
        if (tradingDay == null || tradingDay.isEmpty()) {
            tradingDay = ctpInfo.getTradingDay();
        }
        return Result.success(dataService.readMarket(tradingDay, instrument, index));
    }

    /**
     * 订阅全市场合约
     * @return 订阅的合约数量
     */
    @GetMapping("/subscribeAll")
    public Result<Integer> subscribeAll() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        List<InstrumentEntity> all = tradeService.instruments(null);
        marketService.subscribe(all.stream().map(InstrumentEntity::getInstrumentID).collect(Collectors.toList()));
        return Result.success(all.size());
    }
}
