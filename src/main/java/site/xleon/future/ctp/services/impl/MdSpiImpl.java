package site.xleon.future.ctp.services.impl;

import lombok.EqualsAndHashCode;
import org.apache.commons.io.FileUtils;
import site.xleon.future.ctp.config.CtpInfo;
import ctp.thostmduserapi.*;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.services.Ctp;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 市场行情回调
 */
@EqualsAndHashCode(callSuper = true)
@Component
@Data
@Slf4j
public class MdSpiImpl extends CThostFtdcMdSpi {
    @Autowired
    private CtpInfo ctpInfo;

    @Autowired
    private MarketService marketService;

    @SneakyThrows
    @Override
    public void OnFrontConnected() {
        marketService.setIsConnected(true);
        marketService.setIsLogin(false);
        log.info("market front connected");
        new Thread(()-> {
            try {
                marketService.login();
            } catch (Exception e) {
                log.error("行情登录错误: {}", e.getMessage());
                marketService.setIsLogin(false);
            }
        }).start();
    }

    @Override
    public void OnFrontDisconnected(int reason) {
        log.error("market front disconnected: {}", reason);
        marketService.setIsConnected(false);
        marketService.setIsLogin(false);
    }

    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        Ctp.get(0)
            .append(response -> {
                if (pRspUserLogin.getTradingDay().equals("19800100")) {
                    pRspInfo.setErrorID(-1);
                    pRspInfo.setErrorMsg("行情登录失败");
                    marketService.setIsLogin(false);
                    log.error("行情登录失败: {}", pRspInfo.getErrorMsg());
                }else {
                    marketService.setIsLogin(true);
                    log.info("行情登录成功");
                }
                return pRspUserLogin.getTradingDay();
            })
            .marketFinish(pRspInfo, bIsLast);
    }

    public void onRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        log.info("logout: {} {} {} {}", pUserLogout.getBrokerID(), pUserLogout.getUserID(), pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
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
    @SneakyThrows
    public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField data) {
        Path path = Paths.get("data", ctpInfo.getTradingDay(), data.getInstrumentID() + "_" + ctpInfo.getTradingDay() + ".csv");
        String string = data.getInstrumentID() + ","
                + ctpInfo.getTradingDay() + ","
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

        FileUtils.write(path.toFile(), string, StandardCharsets.UTF_8, true);
    }
    @Override
    public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        super.OnRspError(pRspInfo, nRequestID, bIsLast);
        Ctp.get(nRequestID)
            .append(response -> pRspInfo.getErrorMsg())
            .marketFinish(pRspInfo, bIsLast);
    }
}