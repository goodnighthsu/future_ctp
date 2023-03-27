package site.xleon.future.ctp.models;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("trading_subscribe")
public class TradingSubscribeEntity extends BaseEntity {
    private String instrumentId;
}
