package site.xleon.future.ctp.core.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.core.enums.StateEnum;
import site.xleon.future.ctp.services.impl.MdService;

/**
 *
 */
@Aspect
@Component
public class MarketLoginAspect {
    /**
     * 前置检查
     */
    @Pointcut("within(site.xleon.future.ctp.services.impl.MdService)")
    public void marketNeedFront() {
    }

    @Pointcut("execution(* site.xleon.future.ctp.services.impl.MdService.state())")
    public void marketNoNeedFront() {
    }

    @Pointcut("within(site.xleon.future.ctp.services.impl.MdService)")
    public void needLogin() {
    }

    @Pointcut("execution(* site.xleon.future.ctp.services.impl.MdService.auth()) || " +
            "execution(* site.xleon.future.ctp.services.impl.MdService.login(..)) || " +
            "execution(* site.xleon.future.ctp.services.impl.MdService.state()) || " +
            "execution(* site.xleon.future.ctp.services.impl.MdService.download())")
    public void noNeedLogin() {
    }

    @Before("marketNeedFront() && !marketNoNeedFront()")
    public void before() throws MyException {
        if (StateEnum.SUCCESS != MdService.getConnectState()) {
            throw new MyException("行情前置尚未连接: " + MdService.getConnectState());
        }
    }

    @Before("needLogin() && !noNeedLogin()")
    public void beforeLogin() throws MyException {
        if (StateEnum.SUCCESS != MdService.getConnectState()) {
            throw new MyException("行情前置尚未连接: " + MdService.getConnectState());
        }

        if (StateEnum.SUCCESS != MdService.getLoginState()) {
            throw new MyException("行情用户尚未登录: " + MdService.getLoginState());
        }
    }
}
