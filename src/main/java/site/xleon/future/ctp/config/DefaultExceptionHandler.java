package site.xleon.future.ctp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.http.HttpStatus;
import site.xleon.commons.models.Result;
import site.xleon.future.ctp.core.MyException;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@ControllerAdvice
public class DefaultExceptionHandler extends BasicErrorController {

    public DefaultExceptionHandler() {
        super(new DefaultErrorAttributes(), new ErrorProperties());
    }

    @Override
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Map<String, Object> body = this.getErrorAttributes(request, ErrorAttributeOptions.of(
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.EXCEPTION,
                ErrorAttributeOptions.Include.STACK_TRACE,
                ErrorAttributeOptions.Include.BINDING_ERRORS));
        String error = (String) body.get("error");

        if ("OK".equals(error)) {
            return null;
        }

        String message = (String) body.get("message");
        if (message.equals("No message available")) {
            message = error;
        }
        Result<String> result = Result.fail(message);
        Map<String, Object> map = JSONObject.parseObject(JSON.toJSONString(result), HashMap.class);
        log.error("{}", map);
//        return new ResponseEntity<>(map, getStatus(request));
        return new ResponseEntity<>(map, HttpStatus.OK);
    }

    @ResponseBody
    @ExceptionHandler
    public Result<String> defaultException(HttpServletRequest request, Exception exception) {
        if(exception instanceof MyException) {
            return  Result.fail(exception.getMessage());
        }

        if (exception instanceof UndeclaredThrowableException) {
            Throwable e  = ((UndeclaredThrowableException) exception).getUndeclaredThrowable();
            return Result.fail(e.getMessage());
        }
        return Result.fail(exception.getMessage());
    }
}
