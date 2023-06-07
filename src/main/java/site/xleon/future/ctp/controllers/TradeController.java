package site.xleon.future.ctp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import site.xleon.future.ctp.models.Result;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.impl.TradeService;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;

@Slf4j
@RestController
@RequestMapping("/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

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


}
