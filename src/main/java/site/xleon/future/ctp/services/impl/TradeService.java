package site.xleon.future.ctp.services.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import ctp.thosttraderapi.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.ibatis.session.SqlSessionFactory;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.controllers.TradeController;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.*;
import site.xleon.future.ctp.services.Ctp;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.services.mapper.InstrumentMapper;
import site.xleon.future.ctp.services.mapper.impl.InstrumentService;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("tradingService")
@Slf4j
@Data
public class  TradeService {
    private final AppConfig appConfig;

    private final InstrumentMapper instrumentMapper;

    private final InstrumentService instrumentService;

    private final DataService dataService;
    private final SqlSessionFactory sqlSessionFactory;

    /**
     * 交易日
     */
    private static String tradingDay;
    public static String getTradingDay() {
        if (tradingDay != null) {
            return tradingDay;
        }
        TradeService.tradingDay = TradeService.traderApi.GetTradingDay();
        return TradeService.tradingDay;
    }

    /**
     * 登录用户
     */
    private static UserConfig user;
    public static void setUser(UserConfig item) {
        user = item;
    }

    public static final Object loginLock = new Object();
    public static final Object onLoginStateChange = new Object();
    /**
     * 用户是否登录
     */
    public static volatile StateEnum loginState = StateEnum.DISCONNECT;
    public static StateEnum getLoginState() {
        synchronized (loginLock){
            return loginState;
        }
    }

    /**
     * 更新用户的登录状态
     * @param value 登录状态
     */
    public static void notifyLogin(StateEnum value) {
        synchronized (TradeService.loginLock) {
            loginState = value;
            TradeService.loginLock.notifyAll();
        }
    }

    private static CThostFtdcTraderSpi traderSpi;
    /**
     * 交易api
     */
    public static CThostFtdcTraderApi traderApi;
    static {
        traderApi = CThostFtdcTraderApi.CreateFtdcTraderApi("flow" + File.separator);
        traderSpi = new TraderSpiImpl();
        traderApi.RegisterSpi(traderSpi);
        traderApi.SubscribePublicTopic(ctp.thosttraderapi.THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.SubscribePrivateTopic(ctp.thosttraderapi.THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.Init();
    }

    public static final Object connectLock = new Object();
    /**
     * 前置连接状态
     */
    private static StateEnum connectState = StateEnum.DISCONNECT;
    public static StateEnum getConnectState() {
        return connectState;
    }

    /**
     * 交易前置地址
     */
    private static List<String> fronts = new ArrayList<>();
    public static List<String> getFronts() {
        return fronts;
    }
    /**
     * 连接交易前置
     * @param fronts 前置地址数组 eg: ["tcp://218.202.237.33:10203"]
     * @return 连接状态
     */
    public static StateEnum connectFronts(List<String> fronts) throws MyException, InterruptedException {
        log.info("交易前置 {} 连接", fronts);
        synchronized (connectLock) {
            log.info("connectLock {} state {}", fronts, TradeService.connectState);
            if (StateEnum.LOADING == TradeService.connectState) {
                return TradeService.connectState;
            }
//            TradeService.notifyConnected(StateEnum.LOADING);
            connectState = StateEnum.LOADING;
            TradeService.fronts = fronts;
            if (traderApi != null) {
                traderApi.Release();
            }
            if (traderSpi != null) {
                traderSpi.delete();
            }

            traderApi = CThostFtdcTraderApi.CreateFtdcTraderApi("flow" + File.separator);
            for (String front: fronts) {
                traderApi.RegisterFront(front);
            }
            traderSpi = new TraderSpiImpl();
            traderApi.RegisterSpi(traderSpi);
            traderApi.SubscribePublicTopic(ctp.thosttraderapi.THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
            traderApi.SubscribePrivateTopic(ctp.thosttraderapi.THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
            traderApi.Init();

            while (true) {
                connectLock.wait(3000);
                if (StateEnum.LOADING == connectState) {
                    log.warn("交易前置 {} 连接超时", fronts);
                    TradeService.notifyConnected(StateEnum.DISCONNECT);
                } else {
                    TradeService.notifyConnected(connectState);
                }

                return connectState;
            }
        }
    }

    /**
     * 设置前置已连接
     */
    public static synchronized void notifyConnected(StateEnum value) {
        synchronized (connectLock) {
            connectState = value;
            connectLock.notifyAll();
        }
    }

    /**
     * 交易认证请求
     * @return userId
     */
    public String auth(UserConfig user ) {
        Ctp<String> ctp = new Ctp<>();
        return ctp.request(requestId -> {
            CThostFtdcReqAuthenticateField field = new CThostFtdcReqAuthenticateField();

            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            field.setAppID("simnow_client_test");
            field.setAuthCode("0000000000000000");
            return traderApi.ReqAuthenticate(field, requestId);
        });
    }

    /**
     * 当前系统api状态
     */
    public ApiState state() {
        ApiState state = new ApiState();
        state.setFronts(TradeService.fronts);
        state.setFrontState(TradeService.getConnectState());
        state.setUser(TradeService.user);
        state.setLoginState(TradeService.loginState);
        return state;
    }

    /**
     * ctp 登录
     * @return userId
     */
    public String login(UserConfig user) throws MyException {
        log.info("交易前置，用户 {} 登录 ", user.getUserId());
        if (StateEnum.SUCCESS == TradeService.getLoginState()) {
            throw new MyException("用户已登录，请勿重复登录");
        }
        TradeService.setUser(user);
        Ctp<String> ctp = new Ctp<>();
        ctp.setId(0);
        return ctp.request(requestId -> {
            CThostFtdcReqUserLoginField field = new CThostFtdcReqUserLoginField();
            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            field.setPassword(user.getPassword());
            return traderApi.ReqUserLogin(field, requestId);
        });
    }

    /**
     * 退出登录
     * @param user 用户
     * @return 用户id
     */
    public String logout(UserConfig user) {
        Ctp<String> ctp = new Ctp<>();
        return ctp.request(id -> {
            CThostFtdcUserLogoutField logoutField = new CThostFtdcUserLogoutField();
            logoutField.setBrokerID(user.getBrokerId());
            logoutField.setUserID(user.getUserId());
            return traderApi.ReqUserLogout(logoutField, id);
        });
    }

    /**
     * 更新合约信息库
     */
    public void updateInstrument(List<InstrumentEntity> instruments) {
        // 需要更新的
        List<InstrumentEntity> updates = new ArrayList<>();
        // 需要新增的
        List<InstrumentEntity> adds = new ArrayList<>();
        // 列出数据库所有在交易的合约
        List<InstrumentEntity> tradings = instrumentMapper.listTradings(TradeService.getTradingDay());
        tradings = tradings == null ? new ArrayList<>() : tradings;
        log.info("更新合约状态: {} / {}", instruments.size(), tradings.size());
        for (InstrumentEntity instrument : instruments) {
            // 是否在数据库内
            boolean isFound = false;
            for (InstrumentEntity trading: tradings) {
                if (!instrument.getInstrumentID().equalsIgnoreCase(trading.getInstrumentID())) {
                    continue;
                }
                isFound = true;
                // 交易状态更新， 放到待更新组
                if (instrument.getIsTrading() != trading.getIsTrading()) {
                    instrument.setInstrumentID(trading.getInstrumentID());
                    updates.add(instrument);
                }
            }

            // 不在数据库内，放到带添加合约
            if (!isFound) {
                adds.add(instrument);
            }
        }

        log.info("{} 条新合约", adds.size());
        if (!adds.isEmpty() ) {
            instrumentService.saveBatch(adds);
            log.info("新合约保存成功");
        }

        log.info("{} 条合约状态更新", updates.size());
        if (!updates.isEmpty()) {
            instrumentService.updateBatchById(updates);
            log.info("合约更新成功");
        }
    }

    /**
     * 获取所有交易中的合约
     * @return Instruments
     */
    public List<InstrumentEntity> listTradings() {
        return instrumentMapper.listTradings(TradeService.getTradingDay());
    }

    /**
     * 所有交易中的期货合约
     * @return 期货合约
     */
    public Page<InstrumentEntity> allTradingFutures() throws site.xleon.commons.cql.MyException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        String tradingDay = TradeService.getTradingDay();

        String jsonString = "{\n" +
                "    \"module\": \"instrument\",\n" +
                "    \"filters\": [\n" +
                "         {\n" +
                "            \"key\": \"productClass\",\n" +
                "            \"values\": [\"1\"]\n" +
                "        },\n" +
                "        {\n" +
                "            \"key\": \"expireDate\",\n" +
                "            \"range\": [" + tradingDay + "]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        return dataService.commons(jsonString);
    }

    /**
     * 查询ctp合约
     * @return 合约列表
     */
    public List<InstrumentEntity> listInstruments(String instrument) {
        Ctp<List<InstrumentEntity>> ctp = new Ctp<>();
        CThostFtdcQryInstrumentField instrumentField = new CThostFtdcQryInstrumentField();
        instrumentField.setInstrumentID(instrument);
        return ctp.request(requestId -> traderApi.ReqQryInstrument(instrumentField, requestId));
    }

    public void listPosition() {
        Ctp<String> ctp = new Ctp<>();
        CThostFtdcQryInvestorPositionField positionField = new CThostFtdcQryInvestorPositionField();
        positionField.setBrokerID(user.getBrokerId());
        positionField.setInvestorID(user.getUserId());
        ctp.request(requestId -> traderApi.ReqQryInvestorPosition(positionField, requestId));
    }

    /**
     * 查询资金账户
     */
    public List<TradingAccountEntity> listAccount() {
        Ctp<List<TradingAccountEntity>> ctp = new Ctp<>();
        CThostFtdcQryTradingAccountField field = new CThostFtdcQryTradingAccountField();
        field.setBrokerID(user.getBrokerId());
        field.setInvestorID(user.getUserId());
        return ctp.request(requestId -> traderApi.ReqQryTradingAccount(field, requestId));
    }

    /**
     * 报单
     * @param param 报单参数
     * @return 报单
     */
    public OrderEntity order(TradeController.OrderParam param) {
        CThostFtdcInputOrderField field = new CThostFtdcInputOrderField ();
        field.setBrokerID(user.getBrokerId());
        field.setInvestorID(user.getUserId());
        field.setExchangeID(param.getExchangeId());
        field.setInstrumentID(param.getInstrumentId());
        // 买卖方向 0 买
        field.setDirection(param.getDirection());
        // 开平标志 0 开仓
        field.setCombOffsetFlag(param.getCombOffsetFlag());
        // 投机标志 1 投机
        field.setCombHedgeFlag(param.getCombHedgeFlag());
        // 成交条件 1 立即
        field.setContingentCondition(param.getContingentCondition());
        // 强平原因 0 非强平
        field.setForceCloseReason(param.getForceCloseReason());
        field.setLimitPrice(param.getLimitPrice());
        // 报单价格条件  1任意价 2限价 3最优价 4最新价 ...
        // 上期所只支持限价
        field.setOrderPriceType(param.getOrderPriceType());
        // 成交量类型 1任何数量
        field.setVolumeCondition(param.getVolumeCondition());
        // 有效期类型 1立即完成，否则撤销 2本节有效GFS 3当日有效GFD 4指定日期前有效 5撤销前有效 6集合竞价有效
        field.setTimeCondition(param.getTimeCondition());
        field.setVolumeTotalOriginal(param.getVolumeTotalOriginal());

        Ctp<OrderEntity> ctp = new Ctp<>();
        return ctp.request(requestId -> {
            field.setRequestID(requestId);
            return traderApi.ReqOrderInsert(field, requestId);
        });
    }

    /**
     * 报单查询
     * @return 报单
     */
    public List<CThostFtdcOrderField> listOrder() {
        CThostFtdcQryOrderField field = new CThostFtdcQryOrderField();
        field.setBrokerID(user.getBrokerId());
        field.setInvestorID(user.getUserId());
        Ctp<List<CThostFtdcOrderField>> ctp = new Ctp<>();
        return ctp.request(requestId -> traderApi.ReqQryOrder(field, requestId));
    }

    /**
     * 查询手续费
     */
    public List<CThostFtdcInstrumentCommissionRateField> listCommissionRate(InstrumentEntity instrument) {
        CThostFtdcQryInstrumentCommissionRateField field = new CThostFtdcQryInstrumentCommissionRateField();
        field.setBrokerID(user.getBrokerId());
        field.setInvestorID(user.getUserId());
        field.setInstrumentID(instrument.getInstrumentID());
        field.setExchangeID(instrument.getExchangeInstID());
        Ctp<List<CThostFtdcInstrumentCommissionRateField>> ctp = new Ctp<>();

        return ctp.request(requestId -> traderApi.ReqQryInstrumentCommissionRate(field, requestId));
    }

    /**
     * 保存合约佣金
     */
    public Map<String, List<FeeEntity>> feeCollect() throws IOException, site.xleon.commons.cql.MyException, ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException {
        Page<InstrumentEntity> instruments = this.allTradingFutures();

        HashMap<String, List<FeeEntity>> map = new HashMap<>();
        for(InstrumentEntity instrument: instruments.getRecords()) {
            List<CThostFtdcInstrumentCommissionRateField> rates = listCommissionRate(instrument);
            Thread.sleep(300);
            for (CThostFtdcInstrumentCommissionRateField item: rates) {
                FeeEntity fee = new FeeEntity();
                fee.setExchangeId(instrument.getExchangeID());
                fee.setInstrumentId(instrument.getInstrumentID());
                fee.setCloseRatioByMoney(BigDecimal.valueOf(item.getCloseRatioByMoney()).toString());
                fee.setCloseRatioByVolume(BigDecimal.valueOf(item.getCloseRatioByVolume()).toString());
                fee.setCloseTodayRatioByMoney(BigDecimal.valueOf(item.getCloseTodayRatioByMoney()).toString());
                fee.setCloseTodayRatioByVolume(BigDecimal.valueOf(item.getCloseTodayRatioByVolume()).toString());
                fee.setOpenRatioByMoney(BigDecimal.valueOf(item.getOpenRatioByMoney()).toString());
                fee.setOpenRatioByVolume(BigDecimal.valueOf(item.getOpenRatioByVolume()).toString());

                List<FeeEntity> exchanges = map.computeIfAbsent(fee.getExchangeId(), k -> new ArrayList<>());
                exchanges.add(fee);
            }
        }
        Path path = Paths.get("fee", MdService.getTradingDay() +  "-instrument-fee.json");
        FileUtils.delete(path.toFile());
        dataService.saveJson(map, path);
        return map;
    }

}