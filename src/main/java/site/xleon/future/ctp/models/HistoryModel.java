package site.xleon.future.ctp.models;

import lombok.Data;

/**
 * ctp行情概况
 */
@Data
public class HistoryModel {
    /**
     * 交易日
     */
    private String tradingDay;

    /**
     * 行情数量
     */
    private Integer count;

    /**
     * 文件大小
     */
    private Long size;
}
