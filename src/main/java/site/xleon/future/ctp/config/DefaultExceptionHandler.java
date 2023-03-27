package site.xleon.future.ctp.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import site.xleon.future.ctp.Result;
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
import java.util.HashMap;
import java.util.Map;

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
        Result<String> result = Result.fail(message);
        Map<String, Object> map = JSONObject.parseObject(JSON.toJSONString(result), HashMap.class);
        return new ResponseEntity<>(map, getStatus(request));
    }

    @ResponseBody
    @ExceptionHandler
    public Result<String> defaultException(HttpServletRequest request, Exception exception) {
        return Result.fail(exception.getMessage());
    }
}
