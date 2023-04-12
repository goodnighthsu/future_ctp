package site.xleon.future.ctp.core.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.services.impl.MarketService;

/**
 *
 */
@Aspect
@Component
public class MarketLoginAspect {

    @Autowired
    private MarketService marketService;

    @Pointcut("within(site.xleon.future.ctp.services.impl.MarketService)")
    public void needLogin() {
    }

    @Pointcut("execution(* site.xleon.future.ctp.services.impl.MarketService.auth()) ||  " +
            "execution(* site.xleon.future.ctp.services.impl.MarketService.login()) || " +
            "execution(* site.xleon.future.ctp.services.impl.MarketService.logout()) || " +
            "execution(* site.xleon.future.ctp.services.impl.MarketService.isLogin())")
    public void noNeedLogin() {
    }

    @Before("needLogin() && !noNeedLogin()")
    public void before() throws MyException {
        if (!marketService.isLogin()) {
            throw new MyException("请先登录行情");
        }
    }
}
