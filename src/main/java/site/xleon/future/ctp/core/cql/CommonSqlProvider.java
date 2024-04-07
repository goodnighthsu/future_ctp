package site.xleon.future.ctp.core.cql;

import org.apache.ibatis.builder.annotation.ProviderMethodResolver;

public class CommonSqlProvider implements ProviderMethodResolver {
    public static String list(String sql) {
        return sql;
    }

    public static String count(String sql) { return sql; }
}
