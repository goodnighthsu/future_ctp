package site.xleon.future.ctp.services.mapper;

import site.xleon.future.ctp.models.InstrumentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface IInstrumentMapper extends BaseMapper<InstrumentEntity> {
    /**
     * 获取所有还在交易中的期货合约
     * @param expiryDay 当前交易年月
     * @return 交易中的合约
     */
    @Select("SELECT * FROM instruments WHERE expiry >= #{expiryDay}")
    List<InstrumentEntity> listTradingInstruments(Integer expiryDay);
}
