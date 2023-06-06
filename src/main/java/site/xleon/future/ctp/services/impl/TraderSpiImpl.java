package site.xleon.future.ctp.services.impl;

import lombok.SneakyThrows;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.Ctp;
import ctp.thosttraderapi.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 交易
 */
@Slf4j
@Component
@Data
public class TraderSpiImpl extends CThostFtdcTraderSpi {

    @Autowired
    private CtpInfo ctpInfo;
    @Autowired
    private TradeService tradeService;
    @Autowired
    private DataService dataService;

    @Override
    public void OnRspError(CThostFtdcRspInfoField rspInfoField, int requestID, boolean isLast) {
        log.error("request {} error {}: {}", requestID, rspInfoField.getErrorID(), rspInfoField.getErrorMsg());
    }

    /**
     * 客户端与交易托管连接成功（还未登录）
     */
    @SneakyThrows
    @Override
    public void OnFrontConnected(){
        log.info("交易前置连接成功");
        tradeService.setIsConnected(true);
        tradeService.setIsLogin(false);
        new Thread(()-> {
            try {
                tradeService.login();
            } catch (Exception e) {
                log.error("交易登录失败: {}", e.getMessage());
                tradeService.setIsLogin(false);
            }

        }).start();
    }

    /**
     * 客户端与交易托管连接断开
     * @param nReason 原因
     */
    @Override
    public  void OnFrontDisconnected(int nReason) {
        log.error("front disconnected: {}", nReason);
        tradeService.setIsConnected(false);
        tradeService.setIsLogin(false);
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
                                  boolean bIsLast) {
        Ctp.get(nRequestID)
                .append((response -> pRspAuthenticateField.getUserID()))
                .finish(pRspInfo, bIsLast);
    }

    /**
     * 登录响应
     * @param pRspUserLogin response login
     * @param pRspInfo response info
     * @param nRequestID request id
     * @param bIsLast is last
     */
    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast)
    {
        Ctp.get(nRequestID)
                .append(response -> pRspUserLogin.getUserID())
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
     * @param instrumentField 合约
     * @param infoField 响应信息
     * @param requestId 请求id
     * @param isLast 是否最后一条
     */
    @Override
    public void OnRspQryInstrument(CThostFtdcInstrumentField instrumentField, CThostFtdcRspInfoField infoField, int requestId, boolean isLast) {
//        log.info("instrument: {}", instrumentField.getInstrumentID());
        Ctp.get(requestId)
                .append(response -> {
                    List<InstrumentEntity> instruments = (List<InstrumentEntity>)response;
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