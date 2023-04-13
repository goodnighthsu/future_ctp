package site.xleon.future.ctp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import site.xleon.future.ctp.Result;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.MarketService;
import site.xleon.future.ctp.services.impl.TradeService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private MarketService marketService;

    /**
     * 鉴权
     * @return userId
     */
    @GetMapping("/auth")
    public Result<String> auth() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        String userId = tradeService.auth();
        return Result.success(userId);
    }

    /**
     * 登录
     * @return userId
     */
    @GetMapping("/login")
    public Result<String> login() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        String userId = tradeService.login();
        return Result.success(userId);
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
     *
     * @param tradingDay 交易日， 默认当前交易日
     * @return 合约
     */
    @GetMapping("/instruments")
    public Result<List<InstrumentEntity>> instruments(
            @RequestParam @Nullable String tradingDay,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam @Nullable List<Boolean> subscribes,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "50") Integer pageSize
    ) throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        List<InstrumentEntity> result = tradeService.instruments(tradingDay);
        // 标记是否订阅
        List<String> finalCurrentSubscribes = marketService.getSubscribeInstruments();
        result.forEach(item -> {
            item.setIsSubscribe(finalCurrentSubscribes.contains(item.getInstrumentID()));
        });

        // keyword filter
        result = result.stream().filter(item -> {
            if (keyword != null && keyword.length() > 0) {
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
        int start = Math.min((page - 1) * pageSize, Math.max(total, 0));
        int end = Math.min((start + pageSize), Math.max(total, 0));
        List<InstrumentEntity> sub = result.subList(start, end);
        return Result.success(sub, total);
    }
}
