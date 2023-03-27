package site.xleon.future.ctp.mapper;

import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface IInstrumentService<InstrumentEntity> {

    /**
     * 获取所有交易中的期货合约
     * @return 交易中的期货合约
     */
//    List<InstrumentEntity> listTradingInstruments();

    List<InstrumentEntity> listTradingInstrumentsByExchange(String exchange);
}
