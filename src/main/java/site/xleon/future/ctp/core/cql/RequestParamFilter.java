package site.xleon.future.ctp.core.cql;

import lombok.Data;

/**
 * common请求筛选条件
 */
@Data
public class RequestParamFilter {
    private String key;
    private String value;
    private String[] range;
    private String[] values;
}
