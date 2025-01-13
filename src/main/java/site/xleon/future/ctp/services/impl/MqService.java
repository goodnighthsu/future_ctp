package site.xleon.future.ctp.services.impl;

import com.alibaba.fastjson.JSONObject;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import site.xleon.future.ctp.config.app_config.AppConfig;
import site.xleon.future.ctp.config.app_config.MqConfig;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service("mqService")
@RequiredArgsConstructor
public class MqService {
    private final AppConfig appConfig;

    private Channel channel;
    /**
     * mq 连接
     */
    @Bean
    public void connect() throws IOException, TimeoutException {
        log.info("mq connect");
        MqConfig mqConfig = appConfig.getMq();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(mqConfig.getHost());
        factory.setVirtualHost(mqConfig.getVirtualHost());
        factory.setPassword(mqConfig.getPassword());
        factory.setUsername(mqConfig.getUsername());
        Connection connection = factory.newConnection();
        try {
            channel = connection.createChannel();
            channel.exchangeDeclare("testExchange", "topic");
        } catch (Exception e) {
            log.error("mq connect error", e);
        }
    }

    public void send(String exchange, String routingKey, String msg) throws IOException {
        // exchange, routingKey, props, body
        channel.basicPublish(
                exchange,
                routingKey,
                null,
                msg.getBytes());
    }

    @Data
    private static class Message {
        private String type;
        private Integer id;
    }

    public void listen(String exchange) throws IOException {
        channel.exchangeDeclare(exchange, "topic");
        String queue = channel.queueDeclare().getQueue();
        // binding key broken.account.messageType
        channel.queueBind(queue, exchange, "simu.192003.sendOrder");
        channel.queueBind(queue, exchange, "simu.192003.cancelOrder");
        channel.queueBind(queue, exchange, "simu.192003.*");
        channel.basicConsume(
                queue,
                true,
                (consumerTag, message) -> {
                    Message msg1 = JSONObject.parseObject(message.getBody(), Message.class);
                    log.info("mq receive: {}", msg1);
                    },
                consumerTag -> {
                    log.info("consumerTag: {}", consumerTag);
                });
    }

    /**
     * 创建mq任务
     * 连接 mq, 监听 textExchange
     */
    public void create() throws IOException, TimeoutException {
        log.info("mq task start");
        connect();
        listen("testExchange");
    }
}
