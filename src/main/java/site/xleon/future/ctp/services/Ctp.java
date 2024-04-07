package site.xleon.future.ctp.services;

import ctp.thosttraderapi.CThostFtdcRspInfoField;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import site.xleon.future.ctp.core.IMyFunction;
import site.xleon.future.ctp.core.MyException;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Data
@Slf4j
public class Ctp<T> {
    /**
     * 请求池
     */
    protected static final ConcurrentMap<Integer, Ctp<Object>> REQUESTS = new ConcurrentHashMap<>();

    /**
     * 请求id
     */
    private int id = Math.abs(UUID.randomUUID().hashCode());

    /**
     * 请求锁
     */
    private final Object lock = new Object();

    private int timeout = 6000;

    /**
     * 请求响应
     */
    private T response;

    private int errorId = -999;
    private String errorMsg = "";

    /**
     * promise状态
     */
    private boolean reject = false;

    /**
     * 发起请求
     */
    @SneakyThrows
    public T request(IMyFunction<Integer, Integer>function) {
        log.debug("request {} {} create", id, function.getMethodName());
        Ctp<Object> oldRequest = REQUESTS.get(id);
        if (oldRequest != null) {
            throw new MyException("请求已存在");
        }

        // 将请求放入请求池
        REQUESTS.put(id, (Ctp<Object>) this);

        int code = function.apply(id);
        if (code != 0) {
            REQUESTS.remove(id);
            switch (code) {
                case -1: throw new MyException("网络连接失败");
                case -2: throw new MyException("未处理请求超过许可数");
                case -3: throw new MyException("每秒发送请求数超过许可数");
                default:
                    throw new MyException("request " + id + " fail with code: " + code);
            }
        }

        synchronized (lock) {
            // 等待响应
            lock.wait(timeout);
            REQUESTS.remove(id);
            if (errorId == -999) {
                log.error("request {} {} timeout", function.getMethodName(), id);
                throw new MyException("请求超时");
            }

            if (errorId != 0) {
                throw new MyException("request(" + id + ") error: code(" + errorId + "), " + getErrorMsg());
            }

            log.debug("request({}), finish", id);
            return this.response;
        }
    }

    /**
     * 获取请求
     * @param nRequestID 请求id
     * @return 请求
     */
    public static Ctp<Object> get(int nRequestID) {
        Ctp<Object> request =  REQUESTS.get(nRequestID);
        if (request == null) {
//            log.error("request {} not found", nRequestID);
            request = new Ctp<>();
            request.reject = true;
        }

        return request;
    }

    /**
     * 追加响应
     * @param function 追加函数
     * @return 请求
     */
    public Ctp<T> append(IMyFunction<Object, T> function) {
        if (this.reject) {
            return this;
        }

        T resp = function.apply(this.getResponse());
        this.setResponse(resp);
        return this;
    }

    /**
     * 交易请求完成
     * @param infoField 响应信息
     * @param isFinish 是否完成
     */
    public void finish(CThostFtdcRspInfoField infoField, boolean isFinish) {
        if (this.reject) {
            return;
        }

        if (isFinish) {
            if (infoField != null && infoField.getErrorID() != 0) {
                log.debug("ctp finish request {} error: {}, {}", id, infoField.getErrorID(), infoField.getErrorMsg());
                setErrorId(infoField.getErrorID());
                setErrorMsg(infoField.getErrorMsg());
            }else {
                setErrorId(0);
            }
            synchronized (this.getLock()) {
                this.getLock().notifyAll();
            }
        }
    }

    /**
     * 交易请求完成
     * @param infoField 响应信息
     * @param isFinish 是否完成
     */
    public void marketFinish(ctp.thostmduserapi.CThostFtdcRspInfoField infoField, boolean isFinish) {
        if (reject) {
            return;
        }

        if (isFinish) {
            if (infoField != null && infoField.getErrorID() != 0) {
                log.error("request {} error: {}, {}", id, infoField.getErrorID(), infoField.getErrorMsg());
                setErrorId(infoField.getErrorID());
                setErrorMsg(infoField.getErrorMsg());
            }else {
                setErrorId(0);
            }
            synchronized (this.getLock()) {
                this.getLock().notifyAll();
            }
        }
    }
}
