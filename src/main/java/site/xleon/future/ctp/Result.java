package site.xleon.future.ctp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Integer total;

    public static <T> Result<T> success(T data, Integer total) {
        return new Result<>(1, "success", data, total);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(1, "success", data, null);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(0, message, null, null);
    }
}
