package site.xleon.future.ctp.models;

import lombok.Data;

/**
 * 合约佣金
 */
@Data
public class FeeEntity {
    /**
     * 平仓手续费率
     */
    String closeRatioByMoney;

    /**
     * 平仓手续费
     */
    String closeRatioByVolume;

    /**
     * 平今手续费率
     */
    String closeTodayRatioByMoney;

    /**
     * 平今手续费
     */
    String closeTodayRatioByVolume;

    /**
     * 开仓手续费率
     */
    String openRatioByMoney;

    /**
     * 开仓手续费
     */
    String openRatioByVolume;

    /**
     * 交易所ID
     */
     String exchangeId;

    /**
     * 合约代码
     */
     String instrumentId;

}
