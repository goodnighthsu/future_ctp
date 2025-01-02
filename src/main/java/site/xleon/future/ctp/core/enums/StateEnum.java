package site.xleon.future.ctp.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum StateEnum {
    DISCONNECT(-1,  "离线"),
    SUCCESS(0, "成功"),
    TIMEOUT(1, "连接超时"),
    LOADING(2, "连接中"),
    NETWORK_READ_FAIL(0x1001, " 网络读失败"),
    NETWORK_WRITE_FAIL(0x1002, "网络写失败"),
    RECEIVE_HEARTBEAT_TIMEOUT(0x2001, "接收心跳超时"),
    SEND_HEARTBEAT_FAIL(0x2002, "发送心跳失败"),
    RECEIVE_INVALID_PACK(0x2003, "收到错误报文");

    @Getter
    private final Integer value;
    @Getter
    private final String label;


    private static final Map<Integer, StateEnum> toEnum = new HashMap<>();
    static {
        for(StateEnum reason : StateEnum.values()) {
            toEnum.put(reason.value, reason);
        }
    }

    public static StateEnum byReason(Integer reason) {
        return toEnum.get(reason);
    }
}
