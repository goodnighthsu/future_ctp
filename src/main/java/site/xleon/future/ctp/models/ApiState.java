package site.xleon.future.ctp.models;

import lombok.Data;
import site.xleon.future.ctp.config.app_config.UserConfig;
import site.xleon.future.ctp.core.enums.StateEnum;

import java.util.List;

@Data
public class ApiState {
    private List<String> fronts;
    private StateEnum frontState;

    private UserConfig user;
    private StateEnum loginState;
}
