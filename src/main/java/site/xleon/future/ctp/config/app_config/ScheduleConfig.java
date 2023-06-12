package site.xleon.future.ctp.config.app_config;

import lombok.Data;

/**
 * 计划任务
 */
@Data
public class ScheduleConfig {
    /**
     *
     */
    private Boolean subscribe;

    /**
     * 是否压缩行情数据
     */
    private Boolean marketDataAutoCompress;

    /**
     * 是否下载ctp主服务行情数据
     */
    private Boolean downloadCtpData;
}
