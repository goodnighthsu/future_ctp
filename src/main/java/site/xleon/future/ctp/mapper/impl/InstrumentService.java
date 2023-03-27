package site.xleon.future.ctp.mapper.impl;

import site.xleon.future.ctp.mapper.IInstrumentMapper;
import site.xleon.future.ctp.mapper.IInstrumentService;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.impl.TradingService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("instrumentService")
public class InstrumentService extends ServiceImpl<IInstrumentMapper, InstrumentEntity> implements IInstrumentService {

    @Autowired
    private TradingService tradingService;

    @Override
    public List<InstrumentEntity> listTradingInstrumentsByExchange(String exchange) {
        QueryWrapper<InstrumentEntity> query = new QueryWrapper<>();
        query.select("*")
                .eq("exchange_id", exchange);

        return this.baseMapper.selectList(query);
    }

//    @Override
//    public List<InstrumentEntity> listTradingInstruments() {
//        String tradingDay = tradingService.getTradingDay();
//        String tradingYearMonth = tradingDay.substring(0, 4);
//        return this.baseMapper.listTradingInstruments(Integer.parseInt(tradingYearMonth));
//    }
}
