package site.xleon.future.ctp.config;

import lombok.Data;
import org.springframework.stereotype.Component;
import site.xleon.commons.cql.CommonRelation;

@Data
@Component
public class MyCommonRelation implements CommonRelation {
    /**
     * 数据库表名前缀
     */
    private String tablePrefix = "";

    /**
     * mybatis mapper前缀
     */
    private String mapperPrefix = "site.xleon.future.ctp.services.mapper";

    /**
     * 实体类前缀
     */
    private String modelPrefix = "site.xleon.future.ctp.models";

    private String[][] datas = new String[][]{};
}
