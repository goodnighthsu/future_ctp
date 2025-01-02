package site.xleon.future.ctp.models;

import lombok.Data;
import site.xleon.future.ctp.core.cql.BaseEntity;

@Data
public class OrderEntity extends BaseEntity {
    /**
     * 请求编号
     */
    private Integer requestId;

    /**
     * 本地报单编号
     */
    private String orderLocalId;

    /**
     * 报单提交状态
     */
    private char orderSubmitStatus;

    /**
     * 交易日
     */
    private String tradingDay;

    /**
     * 结算编号
     */
    private Integer settlementId;

    /**
     * 报单编号
     */
    private String orderSysId;

    /**
     * 报单来源
     */
    private char orderSource;

    /**
     * 报单状态
     */
    private char orderStatus;

    /**
     * 状态信息
     */
    private String statusMsg;

    /**
     * 报单类型
     */
    private char orderType;
}
