package site.xleon.future.ctp.services.impl;

import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.models.OrderEntity;
import site.xleon.future.ctp.models.TradingAccountEntity;
import site.xleon.future.ctp.services.Ctp;
import ctp.thosttraderapi.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 交易
 */
@Slf4j
@Data
public class TraderSpiImpl extends CThostFtdcTraderSpi {
    @Override
    public void OnRspError(CThostFtdcRspInfoField rspInfoField, int requestID, boolean isLast) {
        log.error("OnRspError {}: error {}: {}", requestID, rspInfoField.getErrorID(), rspInfoField.getErrorMsg());
        Ctp.get(requestID)
                .finish(rspInfoField, isLast);
    }

    /**
     * 客户端与交易托管连接成功（还未登录）
     */
    @Override
    public void OnFrontConnected() {
        log.info("交易前置 {}, 连接成功", TradeService.getFronts());
        TradeService.notifyConnected(StateEnum.SUCCESS);
    }

    /**
     * 客户端与交易托管连接断开
     *
     * @param nReason 原因
     * @apiNote 当客户端与交易托管系统通信连接断开时，该方法被调用。
     * 当发生这个情况后，API会自动重新连接，客户端可不做处理。
     * 自动重连地址，可能是原来注册的地址，也可能是系统支持的其它可用的通信地址，它由程序自动选择。
     * 注:重连之后需要重新认证、登录
     */
    @Override
    public void OnFrontDisconnected(int nReason) {
        log.warn("交易前置 连接断开: {}", StateEnum.byReason(nReason).getLabel());
        TradeService.notifyConnected(StateEnum.byReason(nReason));
        TradeService.notifyLogin(StateEnum.DISCONNECT);
    }

    @Override
    public void OnHeartBeatWarning(int nTimeLapse) {
        log.error("heart beat over time: {}", nTimeLapse);
    }

    /**
     * 鉴权
     *
     * @param pRspAuthenticateField auth field
     * @param pRspInfo              res info
     * @param nRequestID            request id
     * @param bIsLast               is last
     */
    @Override
    public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField,
                                  CThostFtdcRspInfoField pRspInfo,
                                  int nRequestID,
                                  boolean bIsLast) {
        Ctp.get(nRequestID)
                .append((response -> pRspAuthenticateField.getUserID()))
                .finish(pRspInfo, bIsLast);
    }

    /**
     * 登录响应
     *
     * @param pRspUserLogin response login
     * @param pRspInfo      response info
     * @param nRequestID    request id
     * @param bIsLast       is last
     */
    @Override
    public void OnRspUserLogin(
            CThostFtdcRspUserLoginField pRspUserLogin,
            CThostFtdcRspInfoField pRspInfo,
            int nRequestID,
            boolean bIsLast) {
        log.info("交易前置，用户 {} 登录响应: {}, {}, {}", pRspUserLogin.getUserID(), nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        if (pRspInfo.getErrorID() == 0) {
            TradeService.notifyLogin(StateEnum.SUCCESS);
        } else {
            TradeService.notifyLogin(StateEnum.DISCONNECT);
        }
        Ctp.get(nRequestID)
                .append(response ->
                        pRspUserLogin.getUserID()
                )
                .finish(pRspInfo, bIsLast);
    }

    @Override
    public void OnRspUserLogout(
            CThostFtdcUserLogoutField pRspUserLogout,
            CThostFtdcRspInfoField pRspInfo,
            int nRequestID,
            boolean bIsLast) {
        log.info("user {}: logout {}, {}, {}", pRspUserLogout.getUserID(), nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        TradeService.notifyLogin(StateEnum.DISCONNECT);
        Ctp.get(nRequestID)
                .append(response -> pRspUserLogout.getUserID())
                .finish(pRspInfo, bIsLast);
    }

    @Override
    public void OnRspUserPasswordUpdate(CThostFtdcUserPasswordUpdateField pUserPasswordUpdate,
                                        CThostFtdcRspInfoField pRspInfo, int nRequestID,
                                        boolean bIsLast) {
        log.error("update password: {} {}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
    }

    /**
     * 查询合约响应
     *
     * @param instrumentField 合约
     * @param infoField       响应信息
     * @param requestId       请求id
     * @param isLast          是否最后一条
     */
    @Override
    public void OnRspQryInstrument(CThostFtdcInstrumentField instrumentField, CThostFtdcRspInfoField infoField, int requestId, boolean isLast) {
//        log.info("instrument: {}", instrumentField.getInstrumentID());
        Ctp.get(requestId)
                .append(response -> {
                    List<InstrumentEntity> instruments = (List<InstrumentEntity>) response;
                    if (instruments == null) {
                        instruments = new ArrayList<>();
                    }
                    InstrumentEntity instrument = new InstrumentEntity();
                    instrument.setExchangeID((instrumentField.getExchangeID()));
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

                    if (instrument.getLongMarginRatio() == Double.MAX_VALUE) {
                        instrument.setLongMarginRatio(0);
                    }
                    if (instrument.getShortMarginRatio() == Double.MAX_VALUE) {
                        instrument.setShortMarginRatio(0);
                    }
                    if (instrument.getStrikePrice() == Double.MAX_VALUE) {
                        instrument.setStrikePrice(0);
                    }
                    if (instrument.getUnderlyingMultiple() == Double.MAX_VALUE) {
                        instrument.setUnderlyingMultiple(0);
                    }

                    instruments.add(instrument);
                    return instruments;
                })
                .finish(infoField, isLast);
    }

    public void OnRspQryInvestorPosition(CThostFtdcInvestorPositionField  positionField,
                                         CThostFtdcRspInfoField infoField,
                                         int requestId,
                                         boolean isLast) {
        if (positionField == null) {
            return;
        }
        log.info("broker id: {}",positionField.getBrokerID());
        log.info("broker id: {}",positionField.getInvestorID());
        log.info("broker id: {}",positionField.getPosiDirection());
        log.info("broker id: {}",positionField.getHedgeFlag());
        log.info("broker id: {}",positionField.getPositionDate());
        log.info("broker id: {}",positionField.getYdPosition());
        log.info("broker id: {}",positionField.getPosition());
        log.info("broker id: {}",positionField.getLongFrozen());
        log.info("broker id: {}",positionField.getShortFrozen());
        log.info("broker id: {}",positionField.getOpenVolume());
        log.info("broker id: {}",positionField.getCloseVolume());
        log.info("broker id: {}",positionField.getOpenAmount());
        log.info("broker id: {}",positionField.getCloseVolume());
        log.info("broker id: {}",positionField.getPreMargin());
        log.info("broker id: {}",positionField.getUseMargin());
        log.info("broker id: {}",positionField.getFrozenMargin());
        log.info("broker id: {}",positionField.getFrozenCash());
        log.info("broker id: {}",positionField.getFrozenCommission());
        log.info("broker id: {}",positionField.getCashIn());
        log.info("broker id: {}",positionField.getCommission());
        log.info("broker id: {}",positionField.getCloseProfit());
        log.info("broker id: {}",positionField.getPositionProfit());
        log.info("broker id: {}",positionField.getPreSettlementPrice());
        log.info("broker id: {}",positionField.getSettlementPrice());
        log.info("broker id: {}",positionField.getSettlementID());
        log.info("broker id: {}",positionField.getOpenCost());
        log.info("broker id: {}",positionField.getExchangeMargin());
        log.info("broker id: {}",positionField.getCombPosition());
        log.info("broker id: {}",positionField.getCombLongFrozen());
        log.info("broker id: {}",positionField.getCombShortFrozen());
        log.info("broker id: {}",positionField.getCloseProfitByDate());
        log.info("broker id: {}",positionField.getCloseProfitByTrade());
        log.info("broker id: {}",positionField.getTodayPosition());
        log.info("broker id: {}",positionField.getMarginRateByMoney());
        log.info("broker id: {}",positionField.getMarginRateByVolume());
        log.info("broker id: {}",positionField.getStrikeFrozen());
        log.info("broker id: {}",positionField.getStrikeFrozenAmount());
        log.info("broker id: {}",positionField.getAbandonFrozen());
        log.info("broker id: {}",positionField.getExchangeID());
        log.info("broker id: {}",positionField.getYdStrikeFrozen());
        log.info("broker id: {}",positionField.getInvestUnitID());
        log.info("broker id: {}",positionField.getPositionCostOffset());
        log.info("broker id: {}",positionField.getInstrumentID());

        Ctp.get(requestId)
                .append(response -> {
                    return response;
                })
                .finish(infoField, isLast);
    }

    /**
     * 查询资金账户响应
     * @param field 账号信息
     * @param pRspInfo 响应信息
     * @param nRequestID 请求id
     * @param bIsLast 是否结束
     */
    @Override
    public void OnRspQryTradingAccount(CThostFtdcTradingAccountField field,
                                       CThostFtdcRspInfoField pRspInfo,
                                       int nRequestID,
                                       boolean bIsLast) {
        Ctp.get(nRequestID)
                .append(response -> {
                    List<TradingAccountEntity> result = (List<TradingAccountEntity>) response;
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    if (field == null) {
                        return result;
                    }
                    TradingAccountEntity account = new TradingAccountEntity();
                    account.setBrokerId(field.getBrokerID());
                    account.setAccountId(field.getAccountID());
                    account.setDeposit(field.getDeposit());
                    account.setWithdraw(field.getWithdraw());
                    account.setFrozenMargin(field.getFrozenMargin());
                    account.setFrozenCash(field.getFrozenCash());
                    account.setFrozenCommission(field.getFrozenCommission());
                    account.setCurrMargin(field.getCurrMargin());
                    account.setCashIn(field.getCashIn());
                    account.setCommission(field.getCommission());
                    account.setCloseProfit(field.getCloseProfit());
                    account.setPositionProfit(field.getPositionProfit());
                    account.setAvailable(field.getAvailable());
                    account.setWithdrawQuota(field.getWithdrawQuota());
                    account.setExchangeMargin(field.getExchangeMargin());
                    result.add(account);
                    return result;
                })
                .finish(pRspInfo, bIsLast);
    }

    /**
     * 报单错误时返回
     * @param filed
     * @param pRspInfo
     * @param nRequestID
     * @param bIsLast
     */
    @Override
    public void OnRspOrderInsert(CThostFtdcInputOrderField filed,
                                 CThostFtdcRspInfoField pRspInfo,
                                 int nRequestID,
                                 boolean bIsLast) {
        log.info("on res order insert: {} {}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        Ctp.get(nRequestID)
                .append(response -> {
                    return response;
                })
                .finish(pRspInfo, bIsLast);
    }

    @Override
    public void OnErrRtnOrderInsert(CThostFtdcInputOrderField filed,
                                    CThostFtdcRspInfoField pRspInfo) {
        log.info("order error: {}", filed);
        log.info("resp: {}", pRspInfo);
    }

    @Override
    public void OnRtnOrder(CThostFtdcOrderField pOrder) {
        OrderEntity order = new OrderEntity();
        order.setRequestId(pOrder.getRequestID());
        order.setStatusMsg(pOrder.getStatusMsg());
        order.setOrderLocalId(pOrder.getOrderLocalID());
        order.setOrderSubmitStatus(pOrder.getOrderSubmitStatus());
        order.setTradingDay(pOrder.getTradingDay());
        order.setSettlementId(pOrder.getSettlementID());
        order.setOrderSysId(pOrder.getOrderSysID());
        order.setOrderSource(pOrder.getOrderSource());
        order.setOrderStatus(pOrder.getOrderStatus());
        order.setOrderType(pOrder.getOrderType());
        log.info("order return: {}", order);
        Ctp.get(order.getRequestId())
                .append(response -> order)
                .finish(null, true);
    }

    @Override
    public void OnRtnTrade(CThostFtdcTradeField pTrade) {
        log.info("order trade: {}", pTrade);
    }

    @Override
    public void OnRspQryOrder(CThostFtdcOrderField field,
                              CThostFtdcRspInfoField pRspInfo,
                              int nRequestID,
                              boolean bIsLast) {
        Ctp.get(nRequestID)
                .append(response -> {
                    List<CThostFtdcOrderField> orders = (List<CThostFtdcOrderField>) response;
                    if (orders == null) {
                        orders = new ArrayList<>();
                    }
                    if (field != null) {
                        orders.add(field);
                    }
                    return orders;
                })
                .finish(pRspInfo, bIsLast);
    }

    /**
     * 查询合约收续费响应
     */
    @Override
    public void OnRspQryInstrumentCommissionRate(CThostFtdcInstrumentCommissionRateField  field,
                                                 CThostFtdcRspInfoField pRspInfo,
                                                 int nRequestID,
                                                 boolean bIsLast) {
        Ctp.get(nRequestID)
                .append(response -> {
                    List<CThostFtdcInstrumentCommissionRateField> result = (List<CThostFtdcInstrumentCommissionRateField>) response;
                    if (result == null) {
                        result = new ArrayList<>();
                    }
                    if (field != null) {
                        result.add(field);
                    }
//                    log.info("commission: {}, {}", field.getInstrumentID(), field.getExchangeID());
//                    log.info("{}, {}", String.valueOf(field.getCloseRatioByMoney()), String.valueOf(field.getCloseRatioByVolume()));
//                    log.info("{}, {}", String.valueOf(field.getCloseTodayRatioByMoney()), String.valueOf(field.getCloseTodayRatioByVolume()));
                    return result;
                })
                .finish(pRspInfo, bIsLast);
    }
}