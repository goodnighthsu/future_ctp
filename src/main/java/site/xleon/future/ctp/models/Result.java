package site.xleon.future.ctp.models;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long total;

    public static <T> Result<T> success(T data) {
        return new Result<>(1, "success", data, null);
    }

    public static <T> Result<List<T>> page(Page<T> data) {
        return new Result<>(1, "success", data.getRecords(), data.getTotal());
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(0, message, null, null);
    }
}
