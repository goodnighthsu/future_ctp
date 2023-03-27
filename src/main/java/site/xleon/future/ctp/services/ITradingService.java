package site.xleon.future.ctp.services;

import site.xleon.future.ctp.models.InstrumentEntity;

import java.util.List;

public interface ITradingService {
    /**
     * 全市场合约
     * @return 合约
     */
    List<InstrumentEntity> listAllInstrument();

    /**
     * 全市场期货合约
     * @return 合约
     */
    List<InstrumentEntity> listAllFutures();
}
