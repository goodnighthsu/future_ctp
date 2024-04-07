package site.xleon.future.ctp.core.cql;

import org.apache.ibatis.annotations.SelectProvider;
import java.util.List;

public interface CommonMapper<T> {
    @SelectProvider(CommonSqlProvider.class)
    List<T> list(String sql);

    @SelectProvider(CommonSqlProvider.class)
    Integer count(String sql);
}
