package site.xleon.future.ctp.core;

import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.models.StompMessageModel;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.nio.charset.StandardCharsets;


@Component
public class StompClient {

    private StompSession session;

    private WebSocketStompClient client;

    private AppConfig appConfig;
    @Autowired
    private void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    public void start() {
        StandardWebSocketClient socketClient = new StandardWebSocketClient();
        client = new WebSocketStompClient(socketClient);
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("ws-heartbeat-");
        taskScheduler.afterPropertiesSet();
        client.setTaskScheduler(taskScheduler);
        client.setMessageConverter(new MessageConverter() {
            @Override
            public Object fromMessage(Message<?> message, Class<?> aClass) {
                String jsonString = new String((byte[]) message.getPayload());
                return JSONObject.parseObject(jsonString, StompMessageModel.class);
            }

            @Override
            public Message<?> toMessage(Object o, MessageHeaders messageHeaders) {
                return new Message<byte[]>() {
                    @Override
                    public byte[] getPayload() {return JSON.toJSONString(o).getBytes(StandardCharsets.UTF_8);}

                    @Override
                    public MessageHeaders getHeaders() {return  messageHeaders; }
                };
            }
        });

//        connect();
    }

    public void connect() {
        WebSocketHttpHeaders httpHeaders = new WebSocketHttpHeaders();
        StompHeaders headers = new StompHeaders();
        headers.setLogin("ctp");
        headers.setPasscode(appConfig.getToken());
        headers.setHeartbeat(new long[]{10000L, 10000L});
    }
}
