package site.xleon.future.ctp.services.impl;

import ctp.thostmduserapi.CThostFtdcMdApi;
import ctp.thostmduserapi.CThostFtdcReqUserLoginField;
import ctp.thostmduserapi.CThostFtdcUserLogoutField;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.Ctp;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Service("marketService")
@Slf4j
public class MarketService {
    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CtpInfo ctpInfo;

    @Autowired
    private CThostFtdcMdApi mdApi;

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


    /**
     * 订阅的合约
     */
    private List<String> subscribeInstruments;

    /**
     * 登录
     * @return trading day
     */
    public String login() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        if (!getIsConnected()) {
            throw new MyException("ctp 前置未连接");
        }
        if (getIsLogin()) {
            log.info("market已登录: {}", ctpInfo.getTradingDay());
            return ctpInfo.getTradingDay();
        }
        Ctp<String> ctp = new Ctp<>();
        ctp.setId(0);
        String tradingDay = ctp.request(requestId -> {
            CThostFtdcReqUserLoginField field = new CThostFtdcReqUserLoginField();
            UserConfig user = appConfig.getUser();
            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            field.setPassword(user.getPassword());
            return mdApi.ReqUserLogin(field, requestId);
        });
        ctpInfo.setTradingDay(tradingDay);
        setIsLogin(true);
        synchronized (CtpInfo.loginLock) {
            CtpInfo.loginLock.notifyAll();
            log.info("行情登录成功通知");
        }
        return tradingDay;
    }

    /**
     * 登出
     * @return userId
     */
    public String logout() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        if (!getIsLogin()) {
            return "logout success";
        }
        Ctp<String> ctp = new Ctp<>();
        ctp.setId(0);
        String userId = ctp.request(requestId -> {
            CThostFtdcUserLogoutField field = new CThostFtdcUserLogoutField();
            UserConfig user = appConfig.getUser();
            field.setBrokerID(user.getBrokerId());
            field.setUserID(user.getUserId());
            return mdApi.ReqUserLogout(field, requestId);
        });
        isLogin = false;
        return userId;
    }

    public List<String> getSubscribeInstruments() {
        if (subscribeInstruments == null) {
            subscribeInstruments = dataService.readSubscribe();
        }
        if (subscribeInstruments == null) {
            subscribeInstruments = new ArrayList<>();
        }
        return subscribeInstruments;
    }

    public void setSubscribeInstruments(List<String> subscribeInstruments) {
        // subscribeInstruments 去重
        List<String> _subscribed = subscribeInstruments.stream().distinct().collect(Collectors.toList());
        dataService.saveSubscribe(_subscribed);
        this.subscribeInstruments = _subscribed;
    }


    /**
     * ctp 订阅合约
     */
    public void subscribe(List<String> instruments) {
        log.info("instruments subscribe start");
        if (instruments == null || instruments.isEmpty()) {
            log.warn("instrument subscribe: no instruments found, subscribe skip");
            return;
        }
        // 订阅
        instruments = instruments.stream().distinct().collect(Collectors.toList());
        String[] ids= instruments.toArray(new String[instruments.size()]);
        int code = mdApi.SubscribeMarketData(ids, ids.length);
        log.info("instruments subscribe total {}, {}", ids.length, code);
    }

    /**
     * ctp 取消订阅合约
     */
    public void unsubscribe(List<String> instruments) {
        log.info("instruments unsubscribe start");
        if (instruments.isEmpty()) {
            log.error("instrument unsubscribe failure: no instruments found, unsubscribe skip");
            return;
        }
        // 取消订阅
        String[] ids= instruments.toArray(new String[0]);
        mdApi.UnSubscribeMarketData(ids, ids.length);
        log.info("instruments unsubscribe total {} ", ids.length);
    }
}
