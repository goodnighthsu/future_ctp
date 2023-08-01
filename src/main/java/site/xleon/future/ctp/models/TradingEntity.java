package site.xleon.future.ctp.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor
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
     * 发生时间
     * updateTime + update-Millisec
     */
    private String actionTime;

    public Optional<Date> getActionTimeDate() throws ParseException {
        if (actionTime == null) {
            return Optional.empty();
        }
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return Optional.of(df.parse(actionTime));
    }


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
    private Long tickVolume;

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
    private static final List<String> Schedule1 = new ArrayList<>(Arrays.asList("21:00", "23:00", "9:00", "10:15", "10:30", "11:30", "13:30", "15:00"));
    private static final List<String> Schedule2 = new ArrayList<>(Arrays.asList("9:00", "10:15", "10:30", "11:30", "13:30", "15:00"));
    private static final List<String> Schedule3 = new ArrayList<>(Arrays.asList("21:00", "01:00", "9:00", "10:15", "10:30", "11:30", "13:30", "15:00"));
    private static final List<String> Schedule4 = new ArrayList<>(Arrays.asList("21:00", "02:30", "9:00", "10:15", "10:30", "11:30", "13:30", "15:00"));
    private static final List<String> Schedule5 = new ArrayList<>(Arrays.asList("9:30", "11:30", "13:00", "15:00"));
    private static final List<String> Schedule6 = new ArrayList<>(Arrays.asList("9:15", "11:30", "13:00", "15:15"));
    private static final List<String> Schedule7 = new ArrayList<>(Arrays.asList("9:30", "11:30", "13:00", "15:00"));

    private static final TimeConfig TimeConfig1 = new TimeConfig(Schedule1,
        new ArrayList<>(Arrays.asList(
                "FG", "SA", "MA", "SR", "TA", "RM", "OI", "CF", "CY", "PF", "ZC", // 郑商所
                "i", "j", "jm", "a", "b", "m", "p", "y", "c", "cs", "pp", "v", "eb", "eg", "pg", "rr", "l", // 大连交易所
                "fu", "ru", "bu", "sp", "rb", "hc", // 上期所
                "lu", "nr" // 能源
        ))
    );

    private static final TimeConfig TimeConfig2 = new TimeConfig(Schedule2,
        new ArrayList<>(Arrays.asList(
                    "SM", "SF", "WH", "JR", "LR", "PM", "RI", "RS", "PK", "UR", "CJ", "AP", // 郑商所
                    "bb", "fb", "lh", "jd", // 大连交易所
                    "wr" // 上期所
        ))
    );

    private static final TimeConfig TimeConfig3 = new TimeConfig(Schedule3,
        new ArrayList<>(Arrays.asList(
                "cu", "pb", "al", "zn", "sn", "ni", "ss", // 上期所
                "bc" // 能源
        ))
    );

    private static final TimeConfig TimeConfig4 = new TimeConfig(Schedule4,
        new ArrayList<>(Arrays.asList(
                "au", "ag", // 上期所
                "sc" // 能源
        ))
    );

    private static final TimeConfig TimeConfig5 = new TimeConfig(Schedule5,
        new ArrayList<>(Arrays.asList(
                "IF", "IC", "IH" // 中金
        ))
    );

    private static final TimeConfig TimeConfig6 = new TimeConfig(Schedule6,
        new ArrayList<>(Arrays.asList(
                    "T", "TF", "TS" // 中金
        ))
    );

    private static final List<TimeConfig> TIME_CONFIG = new ArrayList<>(
            Arrays.asList(TimeConfig1, TimeConfig2, TimeConfig3, TimeConfig4, TimeConfig5, TimeConfig6)
    );

    /**
     * 构造
     * @param line 行
     */
    public static TradingEntity createByString(String line) throws ParseException {
        String[] array = line.split(",");
        TradingEntity trading = new TradingEntity();
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd HH:mm:ss.SSS");
//        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        trading.instrumentId = array[0];
        trading.lastPrice = BigDecimal.valueOf(Double.parseDouble(array[6]));
        if (array[23].length() > 0) {
            trading.bidPrice1 = BigDecimal.valueOf(Double.parseDouble(array[23]));
        }
        trading.bidVolume1 = Integer.parseInt(array[24]);
        if (array[25].length() > 0) {
            trading.askPrice1 = BigDecimal.valueOf(Double.parseDouble(array[25]));
        }
        trading.askVolume1 = Integer.parseInt(array[26]);
        trading.actionDay = array[43].split(" ")[0];
        trading.actionTime = array[2] + "." + array[3];
        trading.upperLimitPrice = BigDecimal.valueOf(Double.parseDouble(array[18]));
        trading.lowerLimitPrice = BigDecimal.valueOf(Double.parseDouble(array[19]));
        trading.volume = Double.valueOf(array[13]).longValue();
        if (array[11].length() > 0) {
            trading.highestPrice = BigDecimal.valueOf(Double.parseDouble(array[11]));
        }
        if (array[12].length() > 0) {
            trading.lowestPrice = BigDecimal.valueOf(Double.parseDouble(array[12]));
        }

        trading.preSettlementPrice = BigDecimal.valueOf(Double.parseDouble(array[7]));
        trading.tradingActionTime = df.parse(array[1] + " " + trading.actionTime);
        trading.recvTime = df.parse(array[44]);
        trading.exchangeId = array[4];
        trading.exchangeInstId = array[5];
        if (array[10].length() > 0) {
            trading.openPrice = BigDecimal.valueOf(Double.parseDouble(array[10]));
        }
        trading.openInterest = Double.valueOf(array[15]).longValue();

        return trading;
    }

    public static TradingEntity createByInstrument(String instrument) {
        TradingEntity trading = new TradingEntity();
        trading.setInstrumentId(instrument);
        return trading;
    }

    /**
     * 按合约获取交易时间段
     */
    public List<String> getSchedule() {
        if (this.instrumentId == null) {
            return new ArrayList<>();
        }

        if (this.instrumentId.length() > 6) {
            return Schedule6;
        }

        Pattern pattern = Pattern.compile("^\\w{1,2}");
        Matcher matcher = pattern.matcher(this.instrumentId);
        if (!matcher.find()) {
            return new ArrayList<>();
        }

        String code = matcher.group(0);
        Optional<TimeConfig> finded = TIME_CONFIG.stream()
                .filter(timeConfig -> timeConfig.getProducts().contains(code))
                .findFirst();
        TimeConfig config =  finded.orElseGet(TimeConfig::new);
        return config.getSchedules();
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
    private static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("HH:mm:ss");

    /**
     * 获取交易时间段
     * @param interval 时间间隔
     * @param isIncludeClose 返回时间按段是否包括收盘时间
     * 包含 11:20 11:25 11:30 13:30 13:35
     * 不包含 11:20 11:25 13:30 13:35
     * @return 交易时间段
     */
    @SneakyThrows
    public List<String> getTimeLinesByInterval(Integer interval, Boolean isIncludeClose) {
        List<String> times = new ArrayList<>();
        List<String> schedules = this.getSchedule();
        for (int i = 0; i < schedules.size(); i = i + 2) {
            String openTimeString = schedules.get(i);
            long openTime = dateFormat.parse(openTimeString).getTime();
            String closeTimeString = schedules.get(i+1);
            long closeTime = dateFormat.parse(closeTimeString).getTime();
            if (closeTime < openTime) {
                closeTime += 60*60*24*1000;
            }
            // 1小时 开盘时间不能整除的用上个可整除时间， 收盘时间整除后加上interval
            if (interval == 3600) {
                long mod = openTime % (interval * 1000);
                if (mod != 0) {
                    openTime = openTime - mod;
                }
                mod = closeTime % (interval * 1000);
                if (mod != 0) {
                    closeTime = closeTime - mod + interval * 1000;
                }
            }

            long time = openTime;
            while (time < closeTime) {
                Date date = new Date(time);
                times.add(fullDateFormat.format(date));
                time += interval * 1000;
            }

            if (isIncludeClose) {
                times.add(fullDateFormat.format(new Date(closeTime)));
            }
        }
        // 去重
        return times.stream().distinct().collect(Collectors.toList());
    }
}

/**
 * 交易时间配置
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
class TimeConfig {
    private List<String> schedules = new ArrayList<>();
    private List<String> products = new ArrayList<>();
}
