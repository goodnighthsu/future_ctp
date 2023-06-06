package site.xleon.future.ctp.services.impl;

import ctp.thosttraderapi.CThostFtdcReqAuthenticateField;
import ctp.thosttraderapi.CThostFtdcReqUserLoginField;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.Ctp;
import site.xleon.future.ctp.services.ITradingService;
import ctp.thostmduserapi.CThostFtdcMdApi;
import ctp.thosttraderapi.CThostFtdcQryInstrumentField;
import ctp.thosttraderapi.CThostFtdcTraderApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service("tradingService")
@Slf4j
@Data
public class TradeService implements ITradingService {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CtpInfo ctpInfo;

    @Autowired
    private CThostFtdcMdApi mdApi;

    @Autowired
    private CThostFtdcTraderApi traderApi;

    @Autowired
    private DataService dataService;

    /**
     * 前置是否连接
     */
    private Boolean isConnected = false;

    /**
     * 前置是否登录
     */
    private Boolean isLogin = false;

    public static final Object loginLock = new Object();

    /**
     * 合约缓存
     */
    private List<InstrumentEntity> instruments;

    /**
     *  清除合约缓存
     */
    public void clearInstruments() {
        instruments = null;
    }

    /**
     * 交易认证请求
     * @return userId
     */
    public String auth() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        Ctp<String> ctp = new Ctp<>();
        return ctp.request(requestId -> {
            CThostFtdcReqAuthenticateField field = new CThostFtdcReqAuthenticateField();
            UserConfig user = appConfig.getUser();
            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            field.setAppID("simnow_client_test");
            field.setAuthCode("0000000000000000");
            return traderApi.ReqAuthenticate(field, requestId);
        });
    }

    /**
     * ctp 登录
     * @return userId
     */
    public String login() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        if (!getIsConnected()) {
            throw new MyException("交易前置未连接");
        }
        if (getIsLogin()) {
            log.info("trade已登录: {}", appConfig.getUser().getUserId());
            return appConfig.getUser().getUserId();
        }
        Ctp<String> ctp = new Ctp<>();
        String userId =  ctp.request(requestId -> {
            CThostFtdcReqUserLoginField field = new CThostFtdcReqUserLoginField();
            UserConfig user = appConfig.getUser();
            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            field.setPassword(user.getPassword());
            return traderApi.ReqUserLogin(field, requestId);
        });
        isLogin = true;
        synchronized (CtpInfo.loginLock) {
            CtpInfo.loginLock.notifyAll();
            log.info("交易登录成功通知");
        }
        // 登入成功后，查询合约并保存合约到 subscribe.json文件
        List<InstrumentEntity> aInstruments = listInstruments(null);
        List<String> subscribes = aInstruments.stream()
                .map(InstrumentEntity::getInstrumentID)
                .collect(Collectors.toList());
        dataService.saveSubscribe(subscribes);
        log.info("交易登录成功, 保存{}条合约到 subscribe.json文件", subscribes.size());
        return userId;
    }

    /**
     * 获取交易日全市场合约
     * @param tradingDay 交易日
     * @return 合约
     * @throws MyException exception
     * @throws InvocationTargetException exception
     * @throws NoSuchMethodException exception
     * @throws IllegalAccessException exception
     * @throws InterruptedException exception
     */
    public List<InstrumentEntity> listInstruments(String tradingDay) throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        if (tradingDay == null ) {
            // 从缓存中读取
            if (instruments != null && !instruments.isEmpty()) {
                return instruments;
            }

            // 从本地文件读取
            instruments = dataService.readInstrumentsTradingDay(ctpInfo.getTradingDay());

            if (instruments == null || instruments.isEmpty()) {
                instruments = queryInstruments();
            }
            if (instruments == null) {
                instruments = new ArrayList<>();
            }

            // 保存到本地文件
            dataService.saveInstrumentsTradingDay(instruments, ctpInfo.getTradingDay());

            return instruments;
        }

        // 从本地文件中读取
        List<InstrumentEntity> local =  dataService.readInstrumentsTradingDay(tradingDay);
        if (local == null) {
            local = new ArrayList<>();
        }
        return local;
    }

    /**
     * 查询交易日全市场合约
     * @return 合约
     */
    private List<InstrumentEntity> queryInstruments() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        Ctp<List<InstrumentEntity>> ctp = new Ctp<>();
        return ctp.request(requestId -> {
            CThostFtdcQryInstrumentField field = new CThostFtdcQryInstrumentField();
            return traderApi.ReqQryInstrument(field, requestId);
        });
    }
}