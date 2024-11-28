package site.xleon.future.ctp.services.impl;

import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.models.InstrumentEntity;
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
        TradeService.notifyConnected(StateEnum.byReason(nReason));
        TradeService.notifyLogin(StateEnum.DISCONNECT);
        log.warn("交易前置断开: {}", StateEnum.byReason(nReason).getLabel());
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
}