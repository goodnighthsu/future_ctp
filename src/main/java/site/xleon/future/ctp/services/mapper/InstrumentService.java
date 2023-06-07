package site.xleon.future.ctp.services.mapper;

import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface InstrumentService<InstrumentEntity> {
    List<InstrumentEntity> listTradingInstrumentsByExchange(String exchange);
}
