package site.xleon.future.ctp.core.cql;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.CaseFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.SQL;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import site.xleon.future.ctp.core.MyException;
import site.xleon.future.ctp.core.utils.Utils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询条件
 */
@Data
@Slf4j
public class CommonParam {
    /**
     * 查询对象（表）
     */
    private ModelEnum module;

    /**
     * 筛选条件
     */
    private RequestParamFilter[] filters;

    /**
     * 页码
     */
    private Integer page;

    /**
     * 分页大小
     */
    private Integer pageSize;

    /**
     * 排序字段
     */
    private String sorter;

    /**
     * 排序
     */
    private OrderEnum order;

    /**
     * 校验param
     * @throws InstantiationException exception
     * @throws IllegalAccessException exception
     * @throws MyException exception
     */
    private void check() throws InstantiationException, IllegalAccessException, MyException {
        if (this.getEntityClass() == null) {
            throw new MyException("module: " + this.getModule() + " invalid, module not exist");
        }
        List<Field> fields = this.getEntityClass().newInstance().entityFields();
        List<String> fieldNames = fields.stream().map(Field::getName).collect(Collectors.toList());
        if (sorter != null && !fieldNames.contains(sorter)) {
            throw new MyException("sorter: " + this.sorter + " invalid, sorter not exist");
        }

        if (filters == null) {
            return;
        }

        for (RequestParamFilter filter: filters) {
            if (!fieldNames.contains(filter.getKey())) {
                throw new MyException("filter key: " + filter.getKey() + " invalid, field not exist");
            }
        }
    }

    /**
     * entity class
     * 返回查询对象（表）的实体类
     */
    private Class<? extends BaseEntity> entityClass;
    public Class<? extends BaseEntity> getEntityClass() {
        if (this.getModule() == null) {
            return null;
        }
        String className = this.getModule().getModel();
        Class<? extends BaseEntity> clz = null;
        try {
            clz = (Class<? extends BaseEntity>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.debug("class not found for {}", className);
        }
        return clz;
    }

    /**
     * 获取key的类型
     * 返回查询条件中key对应的表字段的类型
     * @param key key
     * @return class
     */
    private Class<?> getType(String key) {
        Class<?> clz = this.entityClass;
        return Utils.Reflect.getFieldType(clz, key);
    }

    /**
     * key是否number
     * 返回查询条件中key对应的表字段的类型是否是 Number
     * @param key key
     * @return boolean
     */
    private boolean isNumber(String key) {
        Class<?> clz = getType(key);
        if (clz == null) {
            return false;
        }
        return Number.class.isAssignableFrom(clz);
    }

    /**
     * 查询条件的包装
     * 如果查询的字段不是number, 返回的string 加上引号
     * eg: 查询sys_user表account字段 test2001 -> "test2001"
     * 查询sys_user表id字段 1 -> 1
     * @param key key
     * @param range value
     * @return string
     */
    private String getRangeValue1String(String key, String[] range) {
        String keyCamel = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key);
        if (range != null && range.length > 0 && range[0] != null) {
            return getKeyValueString(keyCamel, range[0]);
        }
        return null;
    }

    private String getRangeValue2String(String key, String[] range) {
        String keyCamel = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, key);
        if (range != null && range.length > 1 && range[1] != null) {
            return getKeyValueString(keyCamel, range[1]);
        }
        return null;
    }

    /**
     * 查询条件的包装
     * 如果查询的字段不是number, 返回的string 加上引号
     * eg: 查询sys_user表account字段 test2001 -> "test2001"
     * 查询sys_user表id字段 1 -> 1
     * @param key key
     * @param value value
     * @return string
     */
    private String getKeyValueString(String key, String value) {
        if (isNumber(key)) {
            return value;
        }

        return "'" + value + "'";
    }

    /**
     * 按查询条件创建 sql
     * @return sql
     * @throws MyException exception
     * @throws InstantiationException exception
     * @throws IllegalAccessException exception
     */
    public String createSql() throws MyException, InstantiationException, IllegalAccessException {
        // check
        this.check();

        String tableName = this.getModule().getValue();
        SQL sql = new SQL()
                .SELECT("*");

        // from
        sql = sql.FROM(tableName);

        // join
        sqlJoin(this, sql);

        // where
        sqlWhere(this, sql);

        // order
        sqlOrder(this, sql);

        // page
        sqlPage(this, sql);

        return sql.toString();
    }

    /**
     * 按查询条件创建 统计 sql
     * @return
     */
    public String createCountSql() {
        SQL sql = new SQL()
                .SELECT("count(\"*\")");

        // from
        sql = sql.FROM(this.getModule().getValue());

        // where
        sqlWhere(this, sql);

        return sql.toString();
    }

    private void sqlJoin(CommonParam param, SQL sql) {
        try {
            Class<? extends BaseEntity> entityClass = param.getEntityClass();
            BaseEntity baseEntity = entityClass.newInstance();
            List<RelativeModel> relatives = baseEntity.getRelatives();
            String tableName = BaseEntity.getTableNameByClass(entityClass);
            for (RelativeModel relative : relatives) {
                BaseEntity relativeEntity = relative.getEntityClass().newInstance();

                String relativeTableName = BaseEntity.getTableNameByClass(relative.getEntityClass());

                // 关联查询 select 关联表字段别名
                List<Field> fields = relativeEntity.entityFields();
                StringBuilder sb = new StringBuilder();
                for (Field field: fields) {
                    String lowerUnderscore = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
                    // eg: sys_role.id sys_role_id, sys_role.title sys_role_title
                    sb.append(relativeTableName).append(".").append(lowerUnderscore)
                            .append(" ")
                            .append(relativeTableName).append("_").append(lowerUnderscore)
                            .append(",");
                }
                if (sb.length() == 0) {
                    return;

                }
                sb.deleteCharAt(sb.length() - 1);
                sql.SELECT(sb.toString());

                // join
                // eg: sys_role on sys_user.role_id = sys_role.id
                String joinSb = relativeTableName +
                        " on " +
                        tableName +
                        "." +
                        relative.getId() +
                        " = " +
                        relativeTableName +
                        "." +
                        relative.getForeignId();
                sql.JOIN(joinSb);
            }
        } catch (IllegalAccessException e) {
            log.debug("IllegalAccessException: {}", e.getMessage());
        } catch (InstantiationException e) {
            log.debug("InstantiationException: {}", e.getMessage());
        }
    }

    private void sqlPage(CommonParam param, SQL sql) {
        if (param.getPageSize() == null || param.getPageSize() <= 0) {
            return;
        }
        sql.LIMIT(param.getPageSize());

        if (param.getPage() == null) {
            return;
        }

        long offset = param.getPage() <= 0 ? 0 : param.getPage() - 1;

        sql.OFFSET(offset * param.getPageSize());
    }

    private void sqlOrder(CommonParam param, SQL sql) {
        String tableName = BaseEntity.getTableNameByClass(param.getEntityClass());
        String mySorter = tableName + ".id";

        if (param.getSorter() == null) {
            sql.ORDER_BY(mySorter);
        } else {
            mySorter = tableName + "." + CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, param.getSorter());
            if (param.getOrder() != null) {
                if (OrderEnum.ASCEND == param.getOrder()) {
                    sql.ORDER_BY(mySorter + " ASC");
                } else if (OrderEnum.DESCEND == param.getOrder()) {
                    sql.ORDER_BY(mySorter + " DESC");
                }
            }
        }
    }

    private void sqlWhere (CommonParam param, SQL sql) {
        String tableName = BaseEntity.getTableNameByClass(param.getEntityClass());
        if (param.getFilters() != null) {
            for (RequestParamFilter filter:
                    param.getFilters()) {
                String key = tableName + "." +  CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, filter.getKey());

                // value
                if (filter.getValue() != null) {
                    sql.WHERE(key +  " like \"%" + filter.getValue() +"%\"");
                }

                // range
                if (filter.getRange() != null) {
                    String rangeValue1 = param.getRangeValue1String(key, filter.getRange());
                    String rangeValue2 = param.getRangeValue2String(key, filter.getRange());

                    if (rangeValue1 != null) {
                        sql.AND();
                        sql.WHERE(key + " >= " + rangeValue1);
                    }
                    if (rangeValue2 != null) {
                        sql.WHERE(key + " <= " + rangeValue2);
                    }
                }

                // values
                if (filter.getValues() != null && filter.getValues().length > 0) {
                    // TODO: number
                    List<String> values = Arrays.stream(filter.getValues()).map(value -> "'" + value + "'")
                            .collect(Collectors.toList());
                    sql.WHERE(key + " IN (" + String.join(",", values) + ")");
                }
            }
        }
    }

    /**
     * 返回数据库查询结果
     * @param sqlSessionFactory mybatis sql session
     * @return page result
     * @param <T> any model
     * @throws MyException exception
     * @throws ClassNotFoundException exception
     * @throws InstantiationException exception
     * @throws IllegalAccessException exception
     */
    public <T> Page<T> query(SqlSessionFactory sqlSessionFactory) throws MyException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Page<T> result = new Page<>();
        try (SqlSession session= sqlSessionFactory.openSession()) {
            String className = this.getModule().getMapper();
            Class<?> clz = Class.forName(className);
            CommonMapper<T> mapper = (CommonMapper<T>) session.getMapper(clz);

            // 可以使用反射添加mybatis的map注解，实现动态映射
//            for(Method method : clz.getMethods()) {
//                log.info("method: {}, {}", method.getName(), method.isAnnotationPresent(Results.class));
//                if (method.isAnnotationPresent(Results.class)) {
//                    Results results = method.getAnnotation(Results.class);
//                }
//            }

            // list
            String sql = this.createSql();
            result.setRecords(mapper.list(sql));

            // total
            if (this.getPageSize() != null && this.getPageSize() > 0) {
                String countSql = this.createCountSql();
                result.setSize(this.getPageSize());
                result.setTotal(mapper.count(countSql));
            }
        }

        return result;
    }
}
