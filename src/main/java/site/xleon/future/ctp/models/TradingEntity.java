package site.xleon.future.ctp.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradingEntity {
    /**
     * 接受时间
     */
    private Date recvTime;

    /**
     * 交易发生时间
     * tradingDay + updateTime + update-Millisec
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

    /**
     * 交易时段
     */
    private static final String[] schedule1 = {"21:00", "23:00", "9:00", "10:15", "10:30", "11:30", "13:30", "15:00"};
    private static final String[] schedule2 = {"9:00", "10:15", "10:30", "11:30", "13:30", "15:00"};
    private static final String[] schedule3 = {"21:00", "01:00", "9:00", "10:15", "10:30", "11:30", "13:30", "15:00"};
    private static final String[] schedule4 = {"21:00", "02:30", "9:00", "10:15", "10:30", "11:30", "13:30", "15:00"};
    private static final String[] schedule5 = {"9:30", "11:30", "13:00", "15:00"};
    private static final String[] schedule6 = {"9:15", "11:30", "13:00", "15:15"};
    private static final String[] schedule7 = {"9:30", "11:30", "13:00", "15:00"};

    private static final TimeConfig timeConfig1 = new TimeConfig(schedule1,
            new String[]{"FG", "SA", "MA", "SR", "TA", "RM", "OI", "CF", "CY", "PF", "ZC", // 郑商所
                    "i", "j", "jm", "a", "b", "m", "p", "y", "c", "cs", "pp", "v", "eb", "eg", "pg", "rr", "l", // 大连交易所
                    "fu", "ru", "bu", "sp", "rb", "hc", // 上期所
                    "lu", "nr", // 能源
            }
    );

    /**
     * 构造
     * @param line 行
     */
    public TradingEntity(String line) {
        String[] array = line.split(",");

        this.instrumentId = array[0];
        this.exchangeId = array[4];
        this.exchangeInstId = array[5];
        this.lastPrice = BigDecimal.valueOf(Double.parseDouble(array[6]));
        this.volume = Double.valueOf(array[13]).longValue();
        this.openInterest = Double.valueOf(array[15]).longValue();
    }
}

/**
 * 交易时间配置
 */
@AllArgsConstructor
@Data
class TimeConfig {
    private String[] schedule;
    private String[] products;
}
