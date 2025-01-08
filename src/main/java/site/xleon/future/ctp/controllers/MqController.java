package site.xleon.future.ctp.controllers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.xleon.commons.models.Result;
import site.xleon.future.ctp.services.impl.MqService;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/mq")
@AllArgsConstructor
public class MqController {
    private final MqService mqService;


    @Data
    private static class ConnectParams  {
        private String exchange;
        private String routingKey;
        private Object msg;
    }
    @PostMapping("/send")
    public Result<String> send(@RequestBody ConnectParams params) throws IOException {
        mqService.send(
                params.getExchange(),
                params.getRoutingKey(),
                JSON.toJSONString(params.getMsg()));
        return Result.success("mq send success");
    }
}