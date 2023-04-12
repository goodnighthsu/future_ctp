package site.xleon.future.ctp.services.impl;

import ctp.thosttraderapi.CThostFtdcReqAuthenticateField;
import ctp.thosttraderapi.CThostFtdcReqUserLoginField;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.Ctp;
import site.xleon.future.ctp.services.ITradingService;
import ctp.thostmduserapi.CThostFtdcMdApi;
import ctp.thosttraderapi.CThostFtdcQryInstrumentField;
import ctp.thosttraderapi.CThostFtdcTraderApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private DataService dataService;

    private boolean isLogin = false;

    /**
     * 合约缓存
     */
    private List<InstrumentEntity> instruments;

    /**
     * 交易认证请求
     * @return userId
     */
    public String auth()  {
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
    public String login() {
        if (isLogin) {
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
        return userId;
    }

    public List<InstrumentEntity> instruments (String tradingDay) {
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
    @SneakyThrows
    private List<InstrumentEntity> queryInstruments() {
        Ctp<List<InstrumentEntity>> ctp = new Ctp<>();
        return ctp.request(requestId -> {
            CThostFtdcQryInstrumentField field = new CThostFtdcQryInstrumentField();
            return traderApi.ReqQryInstrument(field, requestId);
        });
    }
}