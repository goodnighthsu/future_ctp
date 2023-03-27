package site.xleon.future.ctp.models;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 合约信息
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("instruments")
public class InstrumentEntity extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    ///交易所代码
    private String exchangeID;
    ///合约名称
    private String instrumentName;
    ///产品类型
    private char productClass;
    ///交割年份
    private int deliveryYear;
    ///交割月
    private int deliveryMonth;
    ///市价单最大下单量
    private int maxMarketOrderVolume;
    ///市价单最小下单量
    private int minMarketOrderVolume;
    ///限价单最大下单量
    private int maxLimitOrderVolume;
    ///限价单最小下单量
    private int minLimitOrderVolume;
    ///合约数量乘数
    private int volumeMultiple;
    ///最小变动价位
    private double priceTick;
    ///创建日
    private String createDate;
    ///上市日
    private String openDate;
    ///到期日
    private String expireDate;
    ///开始交割日
    private String startDelivDate;
    ///结束交割日
    private String endDelivDate;
    ///合约生命周期状态
    private char instLifePhase;
    ///当前是否交易
    private int isTrading;
    ///持仓类型
    private char positionType;
    ///持仓日期类型
    private char positionDateType;
    ///多头保证金率
    private double longMarginRatio;
    ///空头保证金率
    private double shortMarginRatio;
    ///是否使用大额单边保证金算法
    private char maxMarginSideAlgorithm;
    ///执行价
    private double strikePrice;
    ///期权类型
    private char optionsType;
    ///合约基础商品乘数
    private double underlyingMultiple;
    ///组合类型
    private char combinationType;
    ///合约代码
    private String instrumentID;
    ///合约在交易所的代码
    private String exchangeInstID;
    ///产品代码
    private String productID;
    ///基础商品代码
    private String underlyingInstrID;

    private Boolean isSubscribe;
}
