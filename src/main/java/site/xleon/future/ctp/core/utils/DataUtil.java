package site.xleon.future.ctp.core.utils;

import site.xleon.future.ctp.mapper.TradingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class DataUtil {

    private static DataUtil dataUtil;
    @Autowired
    private TradingMapper tradingMapper;

    @PostConstruct
    public void init() {
        dataUtil = this;
        dataUtil.tradingMapper = this.tradingMapper;
    }

    //静态方法
    public static TradingMapper tradingMapper() {
        TradingMapper mapper = DataUtil.dataUtil.tradingMapper;
        return mapper;
    }
}
