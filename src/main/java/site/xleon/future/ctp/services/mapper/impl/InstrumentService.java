package site.xleon.future.ctp.services.mapper.impl;

import site.xleon.future.ctp.services.mapper.InstrumentMapper;
import site.xleon.future.ctp.models.InstrumentEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("instrumentService")
public class InstrumentService extends ServiceImpl<InstrumentMapper, InstrumentEntity> implements site.xleon.future.ctp.services.mapper.InstrumentService {
    @Override
    public List<InstrumentEntity> listTradingInstrumentsByExchange(String exchange) {
        QueryWrapper<InstrumentEntity> query = new QueryWrapper<>();
        query.select("*")
                .eq("exchange_id", exchange);

        return this.baseMapper.selectList(query);
    }
}
