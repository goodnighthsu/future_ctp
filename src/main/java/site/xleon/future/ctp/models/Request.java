package site.xleon.future.ctp.models;

import lombok.Data;

import java.util.concurrent.CountDownLatch;

@Data
public class Request<T> {
    private int requestId;

    private T response;

    private CountDownLatch countDownLatch = new CountDownLatch(1);
}
