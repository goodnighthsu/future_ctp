package site.xleon.future.ctp.services.mapper;

import site.xleon.future.ctp.models.InstrumentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import site.xleon.future.ctp.services.mapper.impl.InstrumentService;

import java.util.List;

@Mapper
public interface InstrumentMapper extends BaseMapper<InstrumentEntity> {
    /**
     * 获取所有还在交易中的期货合约
     * @param expiryDay 当前交易年月
     * @return 交易中的合约
     */
    @Select("SELECT * FROM instrument WHERE expiry >= #{expiryDay}")
    List<InstrumentEntity> listTradingInstruments(Integer expiryDay);

    @Select("SELECT * FROM instrument WHERE instrument_i_d = #{instrumentId}")
    InstrumentEntity getByInstrumentId(String instrumentId);
}
