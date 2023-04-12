package site.xleon.future.ctp.services;

import site.xleon.future.ctp.models.InstrumentEntity;

import java.util.List;

public interface ITradingService {

    /**
     * 登录
     * @return 用户id
     */
    String login();

    /**
     * 交易日全市场合约
     * @return 合约
     */
    List<InstrumentEntity> instruments(String tradingDay);
}
