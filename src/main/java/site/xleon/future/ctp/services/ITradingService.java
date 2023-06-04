package site.xleon.future.ctp.services;

import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.models.InstrumentEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface ITradingService {

    /**
     * 登录
     * @return 用户id
     */
    String login() throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException;

    /**
     * 交易日全市场合约
     * @return 合约
     */
    List<InstrumentEntity> listInstruments(String tradingDay) throws MyException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InterruptedException;
}
