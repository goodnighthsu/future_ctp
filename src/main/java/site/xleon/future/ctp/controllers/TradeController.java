package site.xleon.future.ctp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import site.xleon.future.ctp.Result;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.TradingService;
import ctp.thostmduserapi.CThostFtdcMdApi;
import ctp.thosttraderapi.CThostFtdcQryInstrumentField;
import ctp.thosttraderapi.CThostFtdcQryInvestorPositionField;
import ctp.thosttraderapi.CThostFtdcTraderApi;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
public class TradeController {
    private CThostFtdcTraderApi traderApi;
    @Autowired
    private void setTraderApi(CThostFtdcTraderApi traderApi) {
        this.traderApi = traderApi;
    }

    private CThostFtdcMdApi mdApi;
    @Autowired
    private void setMdApi(CThostFtdcMdApi mdApi) {
        this.mdApi = mdApi;
    }

    @Autowired
    private TradingService tradingService;

    @Autowired
    private CtpInfo ctpInfo;

    @Autowired
    private DataService dataService;

    @GetMapping("/position")
    public void position() {
        CThostFtdcQryInvestorPositionField queryPosition = new CThostFtdcQryInvestorPositionField();
        traderApi.ReqQryInvestorPosition(queryPosition, 4);
    }

    /**
     * 查询合约
     */
    @GetMapping("/instrument/{id}")
    public void instrument(@PathVariable String id) {
        log.info("request instrument: {}", id);
        CThostFtdcQryInstrumentField field = new CThostFtdcQryInstrumentField();
        field.setInstrumentID(id);
        traderApi.ReqQryInstrument(field, 5);
    }

    /**
     * 交易日
     * @return trading day
     */
    @GetMapping("/tradingDay")
    public Result<String> tradingDay() {
        return Result.success(ctpInfo.getTradingDay());
    }


    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/redis")
    public Result<String> redis() {
        redisTemplate.opsForValue().set("redis", "test_redis");
        return Result.success(redisTemplate.opsForValue().get("redis"));
    }

    /**
     * 返回交易日合约
     * @param tradingDay 交易日， 默认当前交易日
     *
     * @return 合约
     * @throws IOException exception
     */
    @GetMapping("/all")
    public Result<List<InstrumentEntity>> allInstruments(
            @RequestParam @Nullable String tradingDay,
            @RequestParam(defaultValue = "")String keyword,
            @RequestParam @Nullable List<Boolean> subscribes,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize
    ) {
        List<InstrumentEntity> result = ctpInfo.getInstruments(tradingDay);
        // 标记是否订阅
        List<String> finalCurrentSubscribes = ctpInfo.getSubscribeInstruments();
        result.forEach(item -> {
            item.setIsSubscribe(finalCurrentSubscribes.contains(item.getInstrumentID()));
        });

        // keyword filter
        result = result.stream().filter(item -> {
            if (keyword != null && keyword.length() > 0 ) {
                return item.getInstrumentID().contains(keyword);
            }
            return true;
        }).filter(item -> {
            if (subscribes != null && !subscribes.isEmpty()) {
                return subscribes.contains(item.getIsSubscribe());
            }
            return true;
        }).collect(Collectors.toList());

        int total = result.size();
        page = Math.max(1, page);
        pageSize = Math.max(1, pageSize);
        int start = Math.min((page-1)*pageSize, Math.max(total, 0));
        int end = Math.min((start + pageSize), Math.max(total, 0));
        List<InstrumentEntity> sub = result.subList(start, end);
        return Result.success(sub, total);
    }

    @GetMapping("/futures")
    public Result<List<InstrumentEntity>> allFutures() {
        return Result.success(tradingService.listAllFutures());
    }

    /**
     * 获取订阅的合约
     * @return subscribe
     */
    @GetMapping("/instrument/subscribe")
    public Result<List<String>> subscribeList()  {
        List<String> instruments = ctpInfo.getSubscribeInstruments();
        mdApi.SubscribeMarketData(instruments.toArray(new String[0]), instruments.size());
        return Result.success(instruments);
    }

    /**
     * 订阅合约
     * @param params 合约id
     * @return 订阅的合约
     */
    @PutMapping("/instrument/subscribe")
    public Result<List<String>> subscribe(@RequestBody List<String> params) {
        List<String> subscribes = ctpInfo.getSubscribeInstruments();
        subscribes.addAll(params);
        tradingService.subscribe(subscribes);
        ctpInfo.setSubscribeInstruments(subscribes);
        return Result.success(params);
    }

    /**
     * 取消订阅
     *
     * @param params 合约id
     * @return 取消订阅的合约
     */
    @PutMapping("/instrument/unsubscribe")
    public Result<List<String>> unsubscribe(@RequestBody List<String> params) {
        tradingService.unsubscribe(params);
        // ctpInfo 移除订阅信息
        List<String> subscribes = ctpInfo.getSubscribeInstruments();
        subscribes.removeAll(params);
        ctpInfo.setSubscribeInstruments(subscribes);
        return Result.success(params);
    }
}
