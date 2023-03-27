package site.xleon.future.ctp.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import lombok.Data;

import java.util.HashMap;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIncludeProperties(value = { "hibernateLazyInitializer", "handler", "fieldHandler"})
public class StompMessageModel {
    private Integer code;
    private String message;
    private String command;

    private HashMap<String, Object> data;
}
