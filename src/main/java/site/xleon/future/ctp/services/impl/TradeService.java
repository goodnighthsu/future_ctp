package site.xleon.future.ctp.services.impl;

import ctp.thosttraderapi.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.ApiState;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.Ctp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.services.mapper.InstrumentMapper;
import site.xleon.future.ctp.services.mapper.impl.InstrumentService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service("tradingService")
@Slf4j
@Data
public class TradeService {
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private InstrumentMapper instrumentMapper;

    @Autowired
    private InstrumentService instrumentService;

    @Autowired
    private DataService dataService;

    /**
     * 登录用户
     */
    private static UserConfig user;
    public static void setUser(UserConfig item) {
        user = item;
    }

    public static final Object loginLock = new Object();
    /**
     * 用户是否登录
     */
    private static volatile StateEnum loginState = StateEnum.DISCONNECT;
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
    private static CThostFtdcTraderApi traderApi;
    static {
        traderApi = CThostFtdcTraderApi.CreateFtdcTraderApi("flow" + File.separator);
        traderSpi = new TraderSpiImpl();
        traderApi.RegisterSpi(traderSpi);
        traderApi.SubscribePublicTopic(ctp.thosttraderapi.THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.SubscribePrivateTopic(ctp.thosttraderapi.THOST_TE_RESUME_TYPE.THOST_TERT_QUICK);
        traderApi.Init();
    }

    private static final Object connectLock = new Object();
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
     * 重置前置连接
     * @param fronts 前置地址数组 eg: ["tcp://218.202.237.33:10203"]
     * @return 连接状态
     */
    public static StateEnum setFronts(List<String> fronts) throws MyException, InterruptedException {
        TradeService.fronts = fronts;
        connectState = StateEnum.DISCONNECT;
        loginState = StateEnum.DISCONNECT;
        if (traderApi != null) {
            traderApi.Release();
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

        synchronized (connectLock) {
            while (StateEnum.DISCONNECT == connectState) {
                connectLock.wait(6000);
                // 超时退出
                if (StateEnum.DISCONNECT == connectState) {
                    throw new MyException(StateEnum.TIMEOUT.getLabel());
                }
            }
        }
        return connectState;
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
        List<InstrumentEntity> tradings = instrumentMapper.listTradings();
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

        log.info("{}条新合约", adds.size());
        if (!adds.isEmpty() ) {
            instrumentService.saveBatch(adds);
            log.info("新合约保存成功");
        }

        log.info("{}条合约状态更新", updates.size());
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
        return instrumentMapper.listTradings();
    }

    /**
     * 查询合约
     * @return 合约列表
     */
    public List<InstrumentEntity> listInstruments(String instrument) {
        Ctp<List<InstrumentEntity>> ctp = new Ctp<>();
        CThostFtdcQryInstrumentField instrumentField = new CThostFtdcQryInstrumentField();
        instrumentField.setInstrumentID(instrument);
        return ctp.request(requestId -> traderApi.ReqQryInstrument(instrumentField, requestId));
    }
}