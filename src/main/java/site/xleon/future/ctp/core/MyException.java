package site.xleon.future.ctp.core;

import lombok.Data;
import site.xleon.future.ctp.core.enums.StateEnum;

@Data
public class MyException extends Exception{
    private StateEnum code;

    public MyException(StateEnum state) {
        super(state.getLabel());
        code = state;
    }

    public MyException(String message) {
        super(message);
    }
}
