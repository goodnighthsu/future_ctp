package site.xleon.future.ctp.core;

import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.mapper.PositionsMapper;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.models.PositionsEntity;
import site.xleon.future.ctp.models.Request;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import ctp.thosttraderapi.*;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * 交易
 */
@Slf4j
@Component
@Data
public class TraderSpiImpl extends CThostFtdcTraderSpi {

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private CThostFtdcTraderApi traderApi;

    @Autowired
    private PositionsMapper positionsMapper;

    private Boolean isLogin = false;

    
    private final Object queryPositionLock = new Object();

    //
    private ConcurrentHashMap<Integer, Request<Object>> requests = new ConcurrentHashMap<>();

    @SneakyThrows
    public <T> T createRequest(int requestId, Function<Integer, Integer> function ) {
        int result = function.apply(requestId);
        if (result != 0) {
            throw new RuntimeException("request failure: " + result);
        }

        Request<Object> request = new Request<>();
        request.setRequestId(requestId);
        requests.put(requestId, request);
        boolean isSuccess = request.getCountDownLatch().await(30, TimeUnit.SECONDS);
        if (!isSuccess) {
            throw new RuntimeException("request timeout");
        }
        return getResponse(requestId);
    }

    public <T> T getResponse(int requestId) {
        Request<T> request = (Request<T>) requests.get(requestId);
        requests.remove(requestId);
        return request.getResponse();
    }


    @Override
    public void OnRspError(CThostFtdcRspInfoField rspInfoField, int requestID, boolean isLast) {
        log.error("request {} error {}: {}", requestID, rspInfoField.getErrorID(), rspInfoField.getErrorMsg());
    }


    /**
     * 客户端与交易托管连接成功（还未登录）
     */
    @Override
    public void OnFrontConnected(){
        log.info("connected success");
        auth();
    }

    private void auth() {
        setIsLogin(false);
        // auth request
        CThostFtdcReqAuthenticateField field = new CThostFtdcReqAuthenticateField();
        UserConfig user = appConfig.getUser();
        field.setBrokerID(user.getBrokerId());
        field.setUserID(user.getUserId());
        field.setAppID("simnow_client_test");
        field.setAuthCode("0000000000000000");
        int code = traderApi.ReqAuthenticate(field,0);
        if (code != 0) {
            log.error("auth failure: {}", code);
            try {
                Thread.sleep(6000);
                auth();
            }catch (InterruptedException exception) {
                log.error("error: ", exception);
            }
        }
        log.info("auth...");
    }


    @Override
    public  void OnFrontDisconnected(int nReason) {
        log.error("front disconnected: {}", nReason);
    }

    /**
     * 鉴权
     * @param pRspAuthenticateField auth field
     * @param pRspInfo res info
     * @param nRequestID request id
     * @param bIsLast is last
     */
    @Override
    public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField,
                                  CThostFtdcRspInfoField pRspInfo,
                                  int nRequestID,
                                  boolean bIsLast)
    {
        if (pRspInfo != null && pRspInfo.getErrorID() != 0)
        {
            log.error("auth failure: {} {}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
            try {
                Thread.sleep(6000);
                auth();
            }catch (InterruptedException exception) {
                log.error("error: ", exception);
            }
            return;
        }
        log.info("auth success;");
        login();
    }

    private void login() {
        // login
        CThostFtdcReqUserLoginField field = new CThostFtdcReqUserLoginField();
        UserConfig user = appConfig.getUser();
        field.setBrokerID(user.getBrokerId());
        field.setUserID(user.getUserId());
        field.setPassword(user.getPassword());
        int code = traderApi.ReqUserLogin(field,0);
        log.info("login...");
        if (code != 0) {
            log.error("auth failure: {}", code);
            try {
                Thread.sleep(6000);
                login();
            }catch (InterruptedException exception) {
                log.error("error: ", exception);
            }
        }
    }


    /**
     * 登录请求
     * @param pRspUserLogin response login
     * @param pRspInfo response info
     * @param nRequestID request id
     * @param bIsLast is last
     */
    @SneakyThrows
    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        if (pRspInfo != null && pRspInfo.getErrorID() != 0)
        {
            log.error("login failure: {} {}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
            // TODO
            if (pRspInfo.getErrorID() == 140 ) {
                // 首次登录需要修改密码
//                CThostFtdcUserPasswordUpdateField  field = new CThostFtdcUserPasswordUpdateField ();
//                UserConfig user = appConfig.getUser();;
//                field.setBrokerID(user.getBrokerId());
//                field.setUserID(user.getUserId());
//                field.setOldPassword("oldpassword");
//                field.setNewPassword("newpassword");
//                m_traderapi.ReqUserPasswordUpdate(field, 0);
//                log.info("update password");
                return;
            }

            return;
        }

        setIsLogin(true);
        log.info("login success");
//         CThostFtdcQryTradingAccountField qryTradingAccount = new CThostFtdcQryTradingAccountField();
//        qryTradingAccount.setBrokerID(m_BrokerId);
//        qryTradingAccount.setCurrencyID(m_CurrencyId);;
//        qryTradingAccount.setInvestorID(m_InvestorId);
        //m_traderapi.ReqQryTradingAccount(qryTradingAccount, 1);

//        CThostFtdcQrySettlementInfoField qrysettlement = new CThostFtdcQrySettlementInfoField();
//        qrysettlement.setBrokerID(m_BrokerId);
//        qrysettlement.setInvestorID(m_InvestorId);
//        qrysettlement.setTradingDay(m_TradingDay);
//        qrysettlement.setAccountID(m_AccountId);
//        qrysettlement.setCurrencyID(m_CurrencyId);
        //m_traderapi.ReqQrySettlementInfo(qrysettlement, 2);

//        CThostFtdcQryInstrumentField qryInstr = new CThostFtdcQryInstrumentField();
//        m_traderapi.ReqQryInstrument(qryInstr, 3);
//        log.info("query instrument");

        // 查询用户持仓任务
//        taskLoadPosition();
    }

    private void taskLoadPosition() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(),
                new CustomizableThreadFactory());
        // 查询用户持仓
        executor.execute(() -> {
            while(true) {
                log.info("query position");
                synchronized (queryPositionLock){
                    log.info("query detail");
                    int queryDetail = (int)new Date().getTime();
                    CThostFtdcQryInvestorPositionDetailField queryPositionDetail = new CThostFtdcQryInvestorPositionDetailField();
                    queryPositionDetail.setBrokerID(appConfig.getUser().getBrokerId());
                    queryPositionDetail.setInvestorID(appConfig.getUser().getUserId());
                    int code = traderApi.ReqQryInvestorPositionDetail(queryPositionDetail, queryDetail);
                    log.info("query detail code: {}", code);
                    log.info("detail wait");
                    try {
                        queryPositionLock.wait(3000);
                    }
                    catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    finally {
                        queryPositionLock.notifyAll();
                    }
                    // 流控
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    /**
     * 用户持仓
     */
    @SneakyThrows
    @Override
    public void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField pInvestorPosition,
                                         CThostFtdcRspInfoField pRspInfo,
                                         int nRequestID,
                                         boolean bIsLast) {
        synchronized(queryPositionLock) {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0) {
                log.error("query position error {}: {}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg() );
            }

            if (pInvestorPosition ==  null) {
                log.info("query position finish with null");
                queryPositionLock.notifyAll();
                return;
            }

            // 合约编号
            String instrument = pInvestorPosition.getInstrumentID();
            // 多空方向
            char direction = pInvestorPosition.getPosiDirection();
            // 手数 (今日持仓）
            int position = pInvestorPosition.getPosition();
            // 开仓金额
            double openAmount = pInvestorPosition.getOpenAmount();
            // 开仓价 (开仓成本)
            double openCost = pInvestorPosition.getOpenCost();
            // 开仓时间 (持仓日期)
            char positionDate = pInvestorPosition.getPositionDate();
            // 持仓类型

            // 成交编号 （结算编号)
            int settlementId = pInvestorPosition.getSettlementID();
            // 持仓盈亏
            double positionProfit = pInvestorPosition.getPositionProfit();
            // 浮动盈亏 （平仓盈亏 ）
            double closeProfit = pInvestorPosition.getCloseProfit();
            // 持仓市值

            // 交易日
            String tradingDay = pInvestorPosition.getTradingDay();

            log.info("position: {} / {} / {} / {} / {} / {} / {} / {} / {}",
                    instrument, direction, position, openAmount, openCost, positionDate, settlementId, positionProfit, closeProfit);
            double marginRate = pInvestorPosition.getMarginRateByMoney();
            log.info("{} / {}", marginRate, tradingDay);

            log.info("{}", bIsLast);
            if (bIsLast) {
                log.info("query position finish");
                queryPositionLock.notifyAll();
            }
        }
    }

    /**
     * 用户持仓详情
     */
    @SneakyThrows
    @Override
    public void OnRspQryInvestorPositionDetail(CThostFtdcInvestorPositionDetailField pInvestorPosition,
                                         CThostFtdcRspInfoField pRspInfo,
                                         int nRequestID,
                                         boolean bIsLast) {
        synchronized (queryPositionLock) {
            if (pRspInfo != null && pRspInfo.getErrorID() != 0) {
                log.error("query position detail error {}: {}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg() );
            }

            if (pInvestorPosition ==  null) {
                log.info("query position detail finish with null");
                clearPosition();
                queryPositionLock.notifyAll();
                return;
            }

            // 合约编号
            String instrument = pInvestorPosition.getInstrumentID();
            // 买卖
            char direction = pInvestorPosition.getDirection();
            // 套保标志
            char hedgeFlag = pInvestorPosition.getHedgeFlag();
            // 成交编号
            String tradeId = pInvestorPosition.getTradeID();
            // 开仓价
            double openPrice = pInvestorPosition.getOpenPrice();
            // 数量
            int volume = pInvestorPosition.getVolume();
            // 开仓日期
            String openDate = pInvestorPosition.getOpenDate();
            // 结算价
            double settlementPrice = pInvestorPosition.getSettlementPrice();

            log.info("detail: {} / {} / {} / {} / {} / {} / {} / {}",
                    instrument, direction, volume, openPrice, openDate, hedgeFlag, tradeId, settlementPrice);

            PositionsEntity positions = new PositionsEntity();
            positions.setBrokerId(appConfig.getUser().getBrokerId());
            positions.setUserId(appConfig.getUser().getUserId());
            positions.setInstrument(instrument);
            positions.setDirection(direction);
            positions.setHedgeFlag(hedgeFlag);
            positions.setTradeId(tradeId);
            positions.setOpenPrice(BigDecimal.valueOf(openPrice));
            positions.setVolume(volume);
            positions.setOpenDate(openDate);

            savePosition(positions);
            if (bIsLast) {
                queryPositionLock.notifyAll();
            }
        }

    }

    /**
     * 清除持仓记录
     */
    private void clearPosition() {
        LambdaQueryWrapper<PositionsEntity> query = new LambdaQueryWrapper<>();
        query.eq(PositionsEntity::getBrokerId, appConfig.getUser().getBrokerId())
            .eq(PositionsEntity::getUserId, appConfig.getUser().getUserId());
        positionsMapper.delete(query);
    }

    /**
     * 更新或新增持仓
     * @param positions 持仓
     */
    private void savePosition(PositionsEntity positions) {
        LambdaQueryWrapper<PositionsEntity> query = new LambdaQueryWrapper<>();
        // 获取用户合约
        query.eq(PositionsEntity::getBrokerId, appConfig.getUser().getBrokerId())
            .eq(PositionsEntity::getUserId, appConfig.getUser().getUserId())
            .eq(PositionsEntity::getInstrument, positions.getInstrument());
        PositionsEntity old = positionsMapper.selectOne(query);
        if (old != null) {
            positions.setId(old.getId());
            positionsMapper.updateById(positions);
        }else{
            positionsMapper.insert(positions);
        }
    }

    @Override
    public void OnRspUserPasswordUpdate(CThostFtdcUserPasswordUpdateField pUserPasswordUpdate,
                                        CThostFtdcRspInfoField pRspInfo, int nRequestID,
                                        boolean bIsLast) {
        log.error("update password: {} {}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
    }

    @Override
    public void OnRspQryInstrument(CThostFtdcInstrumentField instrumentField, CThostFtdcRspInfoField infoField, int requestId, boolean isLast) {
        Request<Object> request = requests.get(requestId);
        List<InstrumentEntity> instruments = (List<InstrumentEntity>)request.getResponse();
        if (instruments == null) {
            instruments = new ArrayList<>();
            request.setResponse(instruments);
        }
        InstrumentEntity instrument = new InstrumentEntity();
        instrument.setExchangeInstID((instrumentField.getExchangeID()));
        instrument.setInstrumentName(instrumentField.getInstrumentName());
        instrument.setProductClass(instrumentField.getProductClass());
        instrument.setDeliveryYear(instrumentField.getDeliveryYear());
        instrument.setDeliveryMonth(instrumentField.getDeliveryMonth());
        instrument.setMaxMarketOrderVolume(instrumentField.getMaxMarketOrderVolume());
        instrument.setMinMarketOrderVolume(instrumentField.getMinMarketOrderVolume());
        instrument.setMaxLimitOrderVolume(instrumentField.getMaxLimitOrderVolume());
        instrument.setMinLimitOrderVolume(instrumentField.getMinLimitOrderVolume());
        instrument.setVolumeMultiple(instrumentField.getVolumeMultiple());
        instrument.setPriceTick(instrumentField.getPriceTick());
        instrument.setCreateDate(instrumentField.getCreateDate());
        instrument.setExpireDate(instrumentField.getExpireDate());
        instrument.setStartDelivDate(instrumentField.getStartDelivDate());
        instrument.setEndDelivDate(instrumentField.getEndDelivDate());
        instrument.setInstLifePhase(instrumentField.getInstLifePhase());
        instrument.setIsTrading(instrumentField.getIsTrading());
        instrument.setPositionType(instrumentField.getPositionType());
        instrument.setPositionDateType(instrumentField.getPositionDateType());
        instrument.setLongMarginRatio(instrumentField.getLongMarginRatio());
        instrument.setShortMarginRatio(instrumentField.getShortMarginRatio());
        instrument.setMaxMarginSideAlgorithm(instrumentField.getMaxMarginSideAlgorithm());
        instrument.setStrikePrice(instrumentField.getStrikePrice());
        instrument.setOptionsType(instrumentField.getOptionsType());
        instrument.setUnderlyingMultiple(instrumentField.getUnderlyingMultiple());
        instrument.setCombinationType(instrumentField.getCombinationType());
        instrument.setInstrumentID(instrumentField.getInstrumentID());
        instrument.setExchangeInstID(instrumentField.getExchangeInstID());
        instrument.setProductID(instrumentField.getProductID());
        instrument.setUnderlyingInstrID(instrumentField.getUnderlyingInstrID());

        instruments.add(instrument);

        if (isLast) {
            request.getCountDownLatch().countDown();
        }
    }
}