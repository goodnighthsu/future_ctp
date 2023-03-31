package site.xleon.future.ctp.services.impl;

import lombok.extern.slf4j.Slf4j;
import site.xleon.future.ctp.core.TraderSpiImpl;
import site.xleon.future.ctp.mapper.IInstrumentMapper;
import site.xleon.future.ctp.models.InstrumentEntity;
import site.xleon.future.ctp.services.ITradingService;
import ctp.thostmduserapi.CThostFtdcMdApi;
import ctp.thosttraderapi.CThostFtdcQryInstrumentField;
import ctp.thosttraderapi.CThostFtdcTraderApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service("tradingService")
@Slf4j
public class TradingService implements ITradingService {

    @Autowired
    private CThostFtdcMdApi mdApi;

    @Autowired
    private TraderSpiImpl traderSpiImpl;

    @Autowired
    private CThostFtdcTraderApi traderApi;

    @Autowired
    private IInstrumentMapper instrumentMapper;


    @Override
    public List<InstrumentEntity> listAllInstrument() {
        int id = (int)System.currentTimeMillis();
        return traderSpiImpl.createRequest(id, (requestId) -> {
            CThostFtdcQryInstrumentField field = new CThostFtdcQryInstrumentField();
            return traderApi.ReqQryInstrument(field, id);
        });
    }

    @Override
    public List<InstrumentEntity> listAllFutures() {
        return listAllInstrument().stream().filter( instrument -> instrument.getInstrumentID().length() < 7).collect(Collectors.toList());
    }

    /**
     * ctp 订阅合约
     */
    public void subscribe(List<String> instruments) {
        log.info("instruments subscribe start");
        if (instruments == null || instruments.isEmpty()) {
            log.warn("instrument subscribe: no instruments found, subscribe skip");
            return;
        }
        // 订阅
        instruments = instruments.stream().distinct().collect(Collectors.toList());
        String[] ids= instruments.toArray(new String[0]);
        mdApi.SubscribeMarketData(ids, ids.length);
        log.info("instruments subscribe total {} ", ids.length);
    }

    /**
     * ctp 取消订阅合约
     */
    public void unsubscribe(List<String> instruments) {
        log.info("instruments unsubscribe start");
        if (instruments.isEmpty()) {
            log.error("instrument unsubscribe failure: no instruments found, unsubscribe skip");
            return;
        }
        // 取消订阅
        String[] ids= instruments.toArray(new String[0]);
        mdApi.UnSubscribeMarketData(ids, ids.length);
        log.info("instruments unsubscribe total {} ", ids.length);
    }
}