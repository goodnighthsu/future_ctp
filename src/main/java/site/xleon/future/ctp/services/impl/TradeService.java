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
import ctp.thosttraderapi.CThostFtdcTraderApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.services.mapper.InstrumentMapper;
import site.xleon.future.ctp.services.mapper.impl.InstrumentService;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

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
    private InstrumentMapper instrumentMapper;

    @Autowired
    private InstrumentService instrumentService;

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
    public String login() throws MyException, InterruptedException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
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

        //
        isLogin = true;
        synchronized (CtpInfo.loginLock) {
            CtpInfo.loginLock.notifyAll();
            log.info("交易登录成功通知");
        }

        return userId;
    }

    /**
     * 更新合约信息库
     */
    public void updateInstrument() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException {
        // 登入成功后，查询合约
        List<InstrumentEntity> aInstruments = listTrading();
        // 需要更新的
        List<InstrumentEntity> updates = new ArrayList<>();
        // 需要新增的
        List<InstrumentEntity> adds = new ArrayList<>();
        // 列出数据库所有在交易的合约
        List<InstrumentEntity> tradings = instrumentMapper.listTradings();
        tradings = tradings == null ? new ArrayList<>() : tradings;
        log.info("check new instrument {} / {}", aInstruments.size(), tradings.size());
        for (InstrumentEntity instrument : aInstruments) {
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
    public List<InstrumentEntity> listTrading() {
        return instrumentMapper.listTradings();
    }
}