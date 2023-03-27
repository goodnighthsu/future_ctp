package site.xleon.future.ctp.models;

import lombok.Data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Date;

@Data
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler"})
public class TradingEntity {
    /**
     * 接受时间
     */
    private Date recvTime;

    /**
     * 交易发生时间
     * tradingDay + updateTime + updateMillisec
     */
    private Date tradingActionTime;

    /**
     * 合约代码
     */
    private String instrumentId;

    /**
     * 交易日期
     */
    private String tradingDay;

    /**
     * 业务日期
     * https://blog.csdn.net/pjjing/article/details/100532276
     * TradingDay用来表示交易日，ActionDay表示当前实际日期。期货交易分为日夜盘，这两个日期在日盘的时候是一致的，但在夜盘就有了区别，是因为当天夜盘是属于第二天这个交易日。例如20190830（周五）晚上21点开始交易，交易日TradingDay是20190902（周一），但实际日期ActionDay是20190830。
     * 这是设计的初衷，但事实上夜盘各交易所这两个日期很混乱
     * 大商所夜盘两个日期都是tradingday，郑商所日夜盘都是当天日期
     */
    private String actionDay;

    /**
     * 最后修改时间
     */
    private String updateTime;

    /**
     * 最后修改毫秒
     */
    private Integer updateMilliSec;

    /**
     * 交易所代码
     */
    private String exchangeId;

    /**
     * 合约在交易所的代码
     */
    private String exchangeInstId;

    /**
     * 最新价
     */
    private BigDecimal lastPrice;

    /**
     * 昨结算价
     */
    private BigDecimal preSettlementPrice;

    /**
     * 昨收盘价
     */
    private BigDecimal preClosePrice;

    /**
     * 昨持仓量
     */
    private Long preOpenInterest;

    /**
     * 今开盘价
     */
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    private BigDecimal highestPrice;

    /**
     * 最低价
     */
    private BigDecimal lowestPrice;

    /**
     * 成交量
     */
    private Long volume;

    /**
     * 成交额
     */
    private BigDecimal turnover;

    /**
     * 持仓量
     */
    private Long openInterest;

    /**
     * 今收盘价
     */
    private BigDecimal closePrice;

    /**
     * 本次结算价
     */
    private BigDecimal settlementPrice;

    /**
     * 涨停板价
     */
    private BigDecimal upperLimitPrice;

    /**
     * 跌停板价
     */
    private BigDecimal lowerLimitPrice;

    /**
     * 昨虚实度
     */
    private BigDecimal preDelta;

    /**
     * 今虚实度
     */
    private BigDecimal currDelta;

    /**
     * 当日均价
     */
    private BigDecimal averagePrice;

    /**
     * 前一tick power
     */
    private Integer preTickPower;

    /**
     * 前一tick成交量
     */
    private Integer preTickVolume;


    /**
     * 前一tick成交均价
     */
    private BigDecimal preTickAvgPrice;

    /**
     * 申买12346 申卖12345
     */
    private BigDecimal bidPrice1;
    private Integer bidVolume1;
    private BigDecimal askPrice1;
    private Integer askVolume1;

    private BigDecimal bidPrice2;
    private Integer bidVolume2;
    private BigDecimal askPrice2;
    private Integer askVolume2;

    private BigDecimal bidPrice3;
    private Integer bidVolume3;
    private BigDecimal askPrice3;
    private Integer askVolume3;

    private BigDecimal bidPrice4;
    private Integer bidVolume4;
    private BigDecimal askPrice4;
    private Integer askVolume4;

    private BigDecimal bidPrice5;
    private Integer bidVolume5;
    private BigDecimal askPrice5;
    private Integer askVolume5;
}
