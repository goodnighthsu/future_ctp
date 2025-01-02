package site.xleon.future.ctp.models;

import lombok.Data;
import site.xleon.future.ctp.core.cql.BaseEntity;

/**
 * 资金账号
 */
@Data
public class TradingAccountEntity  extends BaseEntity {
    private String brokerId;
    private String accountId;
    /**
     * 入金金额
     */
    private double deposit;

    /**
     * 出金金额
     */
    private double withdraw;

    /**
     * 冻结的保证金
     */
    private double frozenMargin;

    /**
     *  冻结的资金
     */
    private double frozenCash;

    /**
     * 冻结的手续费
     */
    private double frozenCommission;

    /**
     * 当前保证金总额
     */
    private double currMargin;

    /**
     * 资金差额
     */
    private double cashIn;

    /**
     * 手续费
     */
    private double commission;

    /**
     * 平仓盈亏
     */
    private double closeProfit;

    /**
     * 持仓盈亏
     */
    private double positionProfit;

    /**
     * 可用资金
     */
    private double available;

    /**
     * 可取资金
     */
    private double withdrawQuota;

    /**
     * 交易所保证金
     */
    private double exchangeMargin;
}
