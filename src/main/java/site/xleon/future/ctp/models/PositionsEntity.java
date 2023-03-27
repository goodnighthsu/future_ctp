package site.xleon.future.ctp.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 持仓
 */
@Data
@TableName("position")
public class PositionsEntity extends BaseEntity{
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String userId;

    private String brokerId;

    private String instrument;

    /**
     * 买卖
     */
    private char direction;

    private Integer volume;

    private BigDecimal openPrice;

    private String openDate;

    /**
     * 投保标志
     */
    private char hedgeFlag;

    /**
     * 成交编号
     */
    private String tradeId;


}
