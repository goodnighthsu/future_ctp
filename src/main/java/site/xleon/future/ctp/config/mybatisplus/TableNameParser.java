package site.xleon.future.ctp.config.mybatisplus;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
public class TableNameParser implements TableNameHandler {

    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();
    public static void setTableName(String tableName) { THREAD_LOCAL.set(String.format("`%s`", tableName));}

    @Override
    public String dynamicTableName(String sql, String tableName) {
        String myTableName = THREAD_LOCAL.get();
        if (myTableName == null) {
            throw new RuntimeException("table name can not be null");
        }

        THREAD_LOCAL.remove();
        return myTableName;
    }
}
