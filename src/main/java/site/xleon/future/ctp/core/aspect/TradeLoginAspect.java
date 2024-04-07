package site.xleon.future.ctp.core.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.services.impl.TradeService;

/**
 * 交易登录切面
 */
@Aspect
@Component
@Slf4j
public class TradeLoginAspect {
    /**
     * 前置检查
     */
    @Pointcut("within(site.xleon.future.ctp.services.impl.TradeService)")
    public void tradeNeedFront() {
    }

    @Pointcut("execution(* site.xleon.future.ctp.services.impl.TradeService.state())")
    public void tradeNoNeedFront() {
    }

    /**
     * 登录检查
     */
    @Pointcut("within(site.xleon.future.ctp.services.impl.TradeService)")
    public void tradeNeedLogin() {
    }

    @Pointcut("execution(* site.xleon.future.ctp.services.impl.TradeService.auth(..)) || " +
            "execution(* site.xleon.future.ctp.services.impl.TradeService.login(..)) || " +
            "execution(* site.xleon.future.ctp.services.impl.TradeService.listTradings()) || " +
            "execution(* site.xleon.future.ctp.services.impl.TradeService.state())")
    public void tradeNoNeedLogin() {
    }

    @Before("tradeNeedFront() && !tradeNoNeedFront()")
    public void before() throws MyException {
        if (StateEnum.SUCCESS != TradeService.getConnectState()) {
            throw new MyException("交易前置尚未连接: " + TradeService.getConnectState());
        }
    }

    @Before("tradeNeedLogin() && !tradeNoNeedLogin()")
    public void beforeLogin() throws MyException {
        if (StateEnum.SUCCESS != TradeService.getConnectState()) {
            throw new MyException("交易前置尚未连接: " + TradeService.getConnectState());
        }

        if (StateEnum.SUCCESS != TradeService.getLoginState()) {
            throw new MyException("交易用户尚未登录: " + TradeService.getLoginState());
        }
    }
}
