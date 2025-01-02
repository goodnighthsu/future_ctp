package site.xleon.future.ctp.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ctp.thosttraderapi.CThostFtdcInstrumentCommissionRateField;
import ctp.thosttraderapi.CThostFtdcOrderField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import site.xleon.commons.cql.MyException;
import site.xleon.commons.models.Result;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.*;
import site.xleon.future.ctp.services.impl.DataService;
import site.xleon.future.ctp.services.impl.TradeService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/trade")
@AllArgsConstructor
public class TradeController {
    private final TradeService tradeService;
    private final DataService dataService;

    // 微服务调用demo
//    private final PlatformService platformService;

    /**
     * 当前交易日
     * @return 交易日
     */
    @GetMapping("/tradingDay")
    public Result<String> tradingDay() {
        return Result.success(TradeService.getTradingDay());
    }

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
     * 连接交易前置
     * @param fronts 交易前置
     * @return 交易前置
     */
    @SneakyThrows
    @PostMapping("/registerFront")
    public Result<String> front(
            @RequestBody List<String> fronts) {
        StateEnum state = TradeService.connectFronts(fronts);
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

    /**
     * 查询ctp合约
     * @param instrument 合约
     * @return 合约信息
     */
    @GetMapping("/ctp/instruments")
    public Result<List<InstrumentEntity>> instruments(
            @RequestParam(required = false) String instrument
    ) {
        List<InstrumentEntity> response = tradeService.listInstruments(instrument);
        return Result.success(response);
    }

    /**
     * 交易中的合约
     * @return  合约信息
     * @throws MyException exception
     * @throws ClassNotFoundException exception
     * @throws InstantiationException exception
     * @throws IllegalAccessException e
     */
    @GetMapping("/tradings")
    public Result<Page<InstrumentEntity>> tradings() throws MyException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String tradingDay = TradeService.getTradingDay();
        String jsonString = "{\n" +
                "    \"module\": \"instrument\",\n" +
                "    \"filters\": [\n" +
                "        {\n" +
                "            \"key\": \"expireDate\",\n" +
                "            \"range\": ["+ tradingDay+ "]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        Page<InstrumentEntity> page = dataService.commons(jsonString);
        return Result.success(page);
    }

    /**
     * 投资者持仓
     */
    @GetMapping("position")
    public Result<String> position() {
        tradeService.listPosition();
        return Result.success("position");
    }

    /**
     * 投资者资金信息
     * @return 资金信息
     */
    @GetMapping("account")
    public Result<List<TradingAccountEntity>> account() {
        List<TradingAccountEntity> accounts = tradeService.listAccount();
        return Result.success(accounts);
    }

    @Data
    public static class OrderParam {
        String exchangeId;
        String instrumentId;
        char direction;
        String combOffsetFlag;
        String combHedgeFlag;
        char contingentCondition;
        char forceCloseReason;
        Double limitPrice;
        char orderPriceType;
        char volumeCondition;
        char timeCondition;
        int volumeTotalOriginal;
    }
    @PostMapping("order")
    public Result<OrderEntity> order(@RequestBody OrderParam field) {
        OrderEntity order = tradeService.order(field);
        return Result.success(order);
    }

    @GetMapping("order")
    public Result<List<CThostFtdcOrderField>> listOrder() {
       List<CThostFtdcOrderField> orders = tradeService.listOrder();
       return Result.success(orders);
    }

    /**
     * 查询合约手续费
     */
    @GetMapping("/instrument/fee")
    public Result<List<CThostFtdcInstrumentCommissionRateField>> fee(
            @RequestParam(required = true) String instrumentId
    ) {
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setInstrumentID(instrumentId);
        List<CThostFtdcInstrumentCommissionRateField> response = tradeService.listCommissionRate(instrument);
        return Result.success(response);
    }

    @GetMapping("/instrument/fee/collect")
    public Result<Map<String, List<FeeEntity>>> feeCollect() throws IOException, MyException, ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException {
        return Result.success(tradeService.feeCollect());
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
