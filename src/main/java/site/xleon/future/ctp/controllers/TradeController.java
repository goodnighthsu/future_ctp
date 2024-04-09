package site.xleon.future.ctp.controllers;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import site.xleon.commons.models.Result;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.ApiState;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.TradeService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/trade")
@AllArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    // 微服务调用demo
//    private final PlatformService platformService;

    @GetMapping("/state")
    public Result<ApiState> state() {
        // 微服务调用demo
//        Result<Object> result = platformService.commonList();
        return Result.success(tradeService.state());
    }

    /**
     * 鉴权
     *
     * @param user user
     * @return userId
     */
    @GetMapping("/auth")
    public Result<String> auth(
            @RequestBody UserConfig user) {
        String userId = tradeService.auth(user);
        return Result.success(userId);
    }

    /**
     * 注册交易前置
     * @param fronts 交易前置
     * @return 交易前置
     */
    @SneakyThrows
    @PostMapping("/registerFront")
    public Result<String> front(
            @RequestBody List<String> fronts) {
        StateEnum state = TradeService.setFronts(fronts);
        return Result.success(state.getLabel());
    }

    /**
     * 登录
     *
     * @return userId
     */
    @SneakyThrows
    @PostMapping("/login")
    public Result<String> login(
            @RequestBody UserConfig user) {
        String userId = tradeService.login(user);
        return Result.success(userId);
    }

    /**
     * 退出登录
     *
     * @return 用户id
     */
    @PostMapping("logout")
    public Result<String> logout(
            @RequestBody UserConfig user) {
        String userId = tradeService.logout(user);
        return Result.success(userId);
    }

    @GetMapping("instruments")
    public Result<List<InstrumentEntity>> instruments(
            @RequestParam(required = false) String instrument
    ) {
        List<InstrumentEntity> response = tradeService.listInstruments(instrument);
        return Result.success(response);
    }

//    @Autowired
//    private StringRedisTemplate redisTemplate;
//
//    @GetMapping("/redis")
//    public Result<String> redis() {
//        redisTemplate.opsForValue().set("redis", "test_redis");
//        return Result.success(redisTemplate.opsForValue().get("redis"));
//    }
}
