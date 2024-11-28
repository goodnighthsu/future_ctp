package site.xleon.future.ctp.services.impl;

import lombok.EqualsAndHashCode;
import org.apache.commons.io.FileUtils;
import ctp.thostmduserapi.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.core.utils.SpringUtils;
import site.xleon.future.ctp.models.TradingEntity;
import site.xleon.future.ctp.services.Ctp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 市场行情回调
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class MdSpiImpl extends CThostFtdcMdSpi {
    @Override
    public void OnFrontConnected() {
        log.info("行情前置 {}: connected", MdService.getFronts());
         MdService.notifyConnected(StateEnum.SUCCESS);
    }

    @Override
    public void OnFrontDisconnected(int nReason) {
        MdService.notifyConnected(StateEnum.byReason(nReason));
        MdService.notifyLogin(StateEnum.DISCONNECT);
        log.error("行情前置断开: {}", StateEnum.byReason(nReason).getLabel());
    }

    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        log.info("行情登录响应 {}: {}, {}, {}", pRspUserLogin.getUserID(), nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        MdService.notifyLogin(StateEnum.SUCCESS);
        Ctp.get(nRequestID)
                .append(response -> pRspUserLogin.getUserID())
                .marketFinish(pRspInfo, bIsLast);
    }

    public void onRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        log.info("登出响应: {} {} {} {}", pUserLogout.getBrokerID(), pUserLogout.getUserID(), pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        MdService.notifyLogin(StateEnum.DISCONNECT);
        Ctp.get(nRequestID)
            .append(response -> pUserLogout.getUserID())
            .marketFinish(pRspInfo, bIsLast);
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
    /**
     * 深度行情通知
     *
     * @param data 行情
     */
    @Override
    public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField data) {
        Path path = Paths.get("data", MdService.getTradingDay(), data.getInstrumentID() + "_" + MdService.getTradingDay() + ".csv");
        String string = data.getInstrumentID() + ","
                + MdService.getTradingDay() + ","
                + data.getUpdateTime() + ","
                + data.getUpdateMillisec() + ","
                + data.getExchangeID() + ","
                + data.getExchangeInstID() + ","
                + data.getLastPrice() + ","
                + data.getPreSettlementPrice() + ","
                + data.getPreClosePrice() + ","
                + data.getPreOpenInterest() + ","
                + data.getOpenPrice() + ","
                + data.getHighestPrice() + ","
                + data.getLowestPrice() + ","
                + data.getVolume() + ","
                + data.getTurnover() + ","
                + data.getOpenInterest() + ","
                + data.getClosePrice() + ","
                + data.getSettlementPrice() + ","
                + data.getUpperLimitPrice() + ","
                + data.getLowerLimitPrice() + ","
                + data.getPreDelta() + ","
                + data.getCurrDelta() + ","
                + data.getAveragePrice() + ","
                + data.getBidPrice1() + ","
                + data.getBidVolume1() + ","
                + data.getAskPrice1() + ","
                + data.getAskVolume1() + ","
                + data.getBidPrice2() + ","
                + data.getBidVolume2() + ","
                + data.getAskPrice2() + ","
                + data.getAskVolume2() + ","
                + data.getBidPrice3() + ","
                + data.getBidVolume3() + ","
                + data.getAskPrice3() + ","
                + data.getAskVolume3() + ","
                + data.getBidPrice4() + ","
                + data.getBidVolume4() + ","
                + data.getAskPrice4() + ","
                + data.getAskVolume4() + ","
                + data.getBidPrice5() + ","
                + data.getBidVolume5() + ","
                + data.getAskPrice5() + ","
                + data.getAskVolume5() + ",";
        string = string.replace("1.7976931348623157E308", "");

        if (data.getTradingDay() != null && data.getUpdateTime() != null  ){
            // trading action time
            String tradingActionTime = String.format("%s %s.%d", data.getActionDay(), data.getUpdateTime(), data.getUpdateMillisec());
            string += tradingActionTime + ",";
        }else{
            string += "null,";
        }

        string += dateFormat.format(new Date(System.currentTimeMillis())) + "\r\n";

        AppConfig appConfig = SpringUtils.getBean("appConfig");
        // 写入行情
        if (appConfig.getSchedule().getSaveQuotation() == null ||
                appConfig.getSchedule().getSaveQuotation()) {
            try {
                FileUtils.write(path.toFile(), string, StandardCharsets.UTF_8, true);
            } catch (IOException e) {
                log.error("行情写入失败: {}", e.getMessage());
            }
        }

        // 保存最新行情
        TradingEntity trading = null;
        try {
            trading = TradingEntity.createByString(string);
        } catch (ParseException e) {
            log.error("行情解析失败: {}", e.getMessage());
        }
        DataService dataService = SpringUtils.getBean("dataService");
        dataService.getQuoteCurrent().put(trading.getInstrumentId(), trading);
    }
    @Override
    public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        super.OnRspError(pRspInfo, nRequestID, bIsLast);
        log.error("OnRspError {}: error {}: {}", nRequestID, pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
        Ctp.get(nRequestID)
            .append(response -> pRspInfo.getErrorMsg())
            .marketFinish(pRspInfo, bIsLast);
    }
}