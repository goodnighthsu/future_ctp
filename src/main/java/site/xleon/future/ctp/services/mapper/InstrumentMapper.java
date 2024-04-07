package site.xleon.future.ctp.services.mapper;

import site.xleon.future.ctp.core.cql.CommonMapper;
import site.xleon.future.ctp.models.InstrumentEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InstrumentMapper extends BaseMapper<InstrumentEntity>,
        CommonMapper<InstrumentEntity> {
    /**
     * 获取所有交易中的期货合约
     * @return 交易中的期货合约
     */
    @Select("SELECT * FROM instrument WHERE is_trading = 1")
    List<InstrumentEntity> listTradings();
}
