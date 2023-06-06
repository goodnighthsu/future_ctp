package site.xleon.future.ctp.core.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.impl.TradeService;

/**
 * 交易登录切面
 */
@Aspect
@Component
public class TradeLoginAspect {

    @Autowired
    private TradeService tradeService;

    @Pointcut("within(site.xleon.future.ctp.services.impl.TradeService)")
    public void tradeNeedLogin() {
    }


    @Pointcut("execution(* site.xleon.future.ctp.services.impl.TradeService.auth()) || " +
            "execution(* site.xleon.future.ctp.services.impl.TradeService.get*(..)) || " +
            "execution(* site.xleon.future.ctp.services.impl.TradeService.set*(..)) || " +
            "execution(* site.xleon.future.ctp.services.impl.TradeService.listInstruments(..)) || " +
            "execution(* site.xleon.future.ctp.services.impl.TradeService.login()) || " +
            "execution(* site.xleon.future.ctp.services.impl.TradeService.isLogin())")
    public void tradeNoNeedLogin() {
    }

    @Before("tradeNeedLogin() && !tradeNoNeedLogin()")
    public void before() throws MyException {
        if (!tradeService.getIsLogin()) {
            throw new MyException("请先登录交易");
        }
    }
}
