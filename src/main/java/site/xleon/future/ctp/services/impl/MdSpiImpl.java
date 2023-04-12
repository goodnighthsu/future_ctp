package site.xleon.future.ctp.services.impl;

import lombok.EqualsAndHashCode;
import org.apache.commons.io.FileUtils;
import site.xleon.future.ctp.config.CtpInfo;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.mybatisplus.TableNameParser;

import site.xleon.future.ctp.mapper.TradingMapper;
import site.xleon.future.ctp.models.TradingEntity;
import ctp.thostmduserapi.*;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.services.Ctp;

import java.math.BigDecimal;
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
    private AppConfig appConfig;

    @Autowired
    private CThostFtdcMdApi mdApi;

    @Autowired
    private TradingMapper tradingMapper;

    @Autowired
    private CtpInfo ctpInfo;

    @Autowired
    private DataService dataService;

    @Autowired
    private TradeService tradeService;

    @Override
    public void OnFrontConnected(){
        ctpInfo.setMarketFrontConnected(true);
        log.info("market front connected");
    }

    @Override
    public void OnFrontDisconnected(int reason) {
        log.error("market front disconnected: {}", reason);
        ctpInfo.setMarketFrontConnected(false);
    }

    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        Ctp.get(0)
            .append(response -> {
                if (pRspUserLogin.getTradingDay().equals("19800100")) {
                    pRspInfo.setErrorID(-1);
                    pRspInfo.setErrorMsg("登录失败");
                    ctpInfo.setMarketLogin(false);
                    log.error("登录失败: {}", pRspInfo.getErrorMsg());
                }else {
                    ctpInfo.setMarketLogin(true);
                    log.info("登录成功");
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
        if (data.getInstrumentID().equals("fu2309")) {
            log.info("{} {} {}", data.getInstrumentID(), data.getUpdateTime(), dateFormat.format(new Date()));
        }
        if (data == null) {
            log.error("depth market data is null");
            return;
        }

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
                + data.getOpenPrice() + ","
                + data.getHighestPrice() + ","
                + data.getLowestPrice() + ","
                + data.getVolume() + ","
                + data.getTurnover() + ","
                + data.getOpenInterest() + ","
                + data.getClosePrice() + ","
                + data.getSettlementPrice() + ","
                + data.getUpperLimitPrice() + ","
                + data.getLowestPrice() + ","
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
        string = string.replace(Double.toString(Double.MAX_VALUE), "");

        if (data.getTradingDay() != null && data.getUpdateTime() != null  ){
            // trading action time
            String tradingActionTime = String.format("%s %s.%d", data.getActionDay(), data.getUpdateTime(), data.getUpdateMillisec());
//            Date date = dateFormat.parse(tradingActionTime);
            string += tradingActionTime + ",";
        }else{
            string += "null,";
        }

        string += dateFormat.format(new Date(System.currentTimeMillis())) + "\r\n";

        FileUtils.write(path.toFile(),  string, StandardCharsets.UTF_8, true);
//        if (data.getInstrumentID().equals("fu2305")) {
//            log.info("market data save: {} {}", data.getInstrumentID(), data.getUpdateTime());
//        }
    }

    private SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
    @SneakyThrows
    private void add(CThostFtdcDepthMarketDataField data) {
        if (data == null) {
            return;
        }
//        long start = System.currentTimeMillis();

        // 合约名
        String tableName = "trading_" + data.getInstrumentID();
        boolean isExist = tradingMapper.existTable(appConfig.getMysql().getDatabase(), tableName) > 0;
        if (!isExist) {
            tradingMapper.createTable(tableName);
        }

        TableNameParser.setTableName(tableName);
        TradingEntity trading = new TradingEntity();
        trading.setInstrumentId(data.getInstrumentID());
        // api给到的郑商所的夜盘交易日没有调整， 所有交易日使用mdApi返回的交易日
        trading.setTradingDay(ctpInfo.getTradingDay());

        trading.setActionDay(data.getActionDay());
        trading.setUpdateTime(data.getUpdateTime());
        trading.setUpdateMilliSec(data.getUpdateMillisec());
        trading.setExchangeId(data.getExchangeID());
        trading.setExchangeInstId(data.getExchangeInstID());
        if (data.getLastPrice() != Double.MAX_VALUE) {
            trading.setLastPrice(BigDecimal.valueOf(data.getLastPrice()));
        }

        if (data.getPreSettlementPrice() != Double.MAX_VALUE) {
            trading.setPreSettlementPrice(BigDecimal.valueOf(data.getPreSettlementPrice()));
        }

        if (data.getPreClosePrice() != Double.MAX_VALUE) {
            trading.setPreClosePrice(BigDecimal.valueOf(data.getPreClosePrice()));
        }

        if (data.getOpenPrice() != Double.MAX_VALUE) {
            trading.setOpenPrice(BigDecimal.valueOf(data.getOpenPrice()));
        }

        if (data.getHighestPrice() != Double.MAX_VALUE) {
            trading.setHighestPrice(BigDecimal.valueOf(data.getHighestPrice()));
        }

        if (data.getLowestPrice() != Double.MAX_VALUE) {
            trading.setLowestPrice(BigDecimal.valueOf(data.getLowestPrice()));
        }

        trading.setVolume((long) data.getVolume());

        if (data.getTurnover() != Double.MAX_VALUE) {
            trading.setTurnover(BigDecimal.valueOf(data.getTurnover()));
        }

        trading.setOpenInterest((long) data.getOpenInterest());

        if (data.getClosePrice() != Double.MAX_VALUE) {
            trading.setClosePrice(BigDecimal.valueOf(data.getClosePrice()));
        }

        if (data.getSettlementPrice() != Double.MAX_VALUE) {
            trading.setSettlementPrice(BigDecimal.valueOf(data.getSettlementPrice()));
        }

        if (data.getUpperLimitPrice() != Double.MAX_VALUE) {
            trading.setUpperLimitPrice(BigDecimal.valueOf(data.getUpperLimitPrice()));
        }

        if (data.getLowestPrice() != Double.MAX_VALUE) {
            trading.setLowerLimitPrice(BigDecimal.valueOf(data.getLowestPrice()));
        }

        if (data.getPreDelta() != Double.MAX_VALUE) {
            trading.setPreDelta(BigDecimal.valueOf(data.getPreDelta()));
        }

        if (data.getCurrDelta() != Double.MAX_VALUE) {
            trading.setCurrDelta(BigDecimal.valueOf(data.getCurrDelta()));
        }

        if (data.getAveragePrice() != Double.MAX_VALUE) {
            trading.setAveragePrice(BigDecimal.valueOf(data.getAveragePrice()));
        }

        if (data.getBidPrice1() != Double.MAX_VALUE) {
            trading.setBidPrice1(BigDecimal.valueOf(data.getBidPrice1()));
        }

        trading.setBidVolume1(data.getBidVolume1());

        if (data.getAskPrice1() != Double.MAX_VALUE) {
            trading.setAskPrice1(BigDecimal.valueOf(data.getAskPrice1()));
        }

        trading.setAskVolume1(data.getAskVolume1());

        if (data.getBidPrice2() != Double.MAX_VALUE) {
            trading.setBidPrice2(BigDecimal.valueOf(data.getBidPrice2()));
        }

        trading.setBidVolume2(data.getBidVolume2());

        if (data.getAskPrice2() != Double.MAX_VALUE) {
            trading.setAskPrice2(BigDecimal.valueOf(data.getAskPrice2()));
        }

        trading.setAskVolume2(data.getAskVolume2());

        if (data.getBidPrice3() != Double.MAX_VALUE) {
            trading.setBidPrice3(BigDecimal.valueOf(data.getBidPrice3()));
        }

        trading.setBidVolume3(data.getBidVolume3());

        if (data.getAskPrice3() != Double.MAX_VALUE) {
            trading.setAskPrice3(BigDecimal.valueOf(data.getAskPrice3()));
        }

        trading.setAskVolume3(data.getAskVolume3());

        if (data.getBidPrice4() != Double.MAX_VALUE) {
            trading.setBidPrice4(BigDecimal.valueOf(data.getBidPrice4()));
        }

        trading.setBidVolume4(data.getBidVolume4());

        if (data.getAskPrice4() != Double.MAX_VALUE) {
            trading.setAskPrice4(BigDecimal.valueOf(data.getAskPrice4()));
        }

        trading.setAskVolume4(data.getAskVolume4());

        if (data.getBidPrice5() != Double.MAX_VALUE) {
            trading.setBidPrice5(BigDecimal.valueOf(data.getBidPrice5()));
        }

        trading.setBidVolume5(data.getBidVolume5());

        if (data.getAskPrice5() != Double.MAX_VALUE) {
            trading.setAskPrice5(BigDecimal.valueOf(data.getAskPrice5()));
        }

        trading.setAskVolume5(data.getAskVolume5());

        trading.setRecvTime(new Date(System.currentTimeMillis()));

        if (data.getTradingDay() != null && data.getUpdateTime() != null  ){
            String tradingActionTime = String.format("%s %s.%d", data.getActionDay(), data.getUpdateTime(), data.getUpdateMillisec());
            SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
            Date date = df.parse(tradingActionTime);
            trading.setTradingActionTime(date);

//            if (data.getUpdateMillisec() == 0){
//                List<TradingEntity> old = tradingMapper.listByActionTime(tableName, date);
//                if (old != null && !old.isEmpty()) {
//                    long timeInterval = date.getTime() + 500;
//                    trading.setTradingActionTime(new Date(timeInterval));
//                    trading.setUpdateMilliSec(500);
//                }
//            }
        }

        // 收到数据的实际交易时间大于当前时间1小时的作为垃圾数据，跳过
        long delta =  trading.getTradingActionTime().getTime() - System.currentTimeMillis();
        if ( delta > 60.0 * 1000.0 * 60.0) {
            return;
        }

        int count = tradingMapper.insert(trading);
        if (count != 1) {
            log.error("trading add failure");
        }
    }

    @Override
    public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        super.OnRspError(pRspInfo, nRequestID, bIsLast);
        Ctp.get(nRequestID)
            .append(response -> pRspInfo.getErrorMsg())
            .marketFinish(pRspInfo, bIsLast);
    }
}