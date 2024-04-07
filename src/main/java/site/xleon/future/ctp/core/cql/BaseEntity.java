package site.xleon.future.ctp.core.cql;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * base entity
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(value = {
        "hibernateLazyInitializer", "handler", "fieldHandler", "flag", "tableName", "relatives"
})
@Data
public class BaseEntity {

    @JsonFormat(pattern = DEFAULT_DATE_PATTERN, timezone = DEFAULT_TIMEZONE)
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern = DEFAULT_DATE_PATTERN, timezone = DEFAULT_TIMEZONE)
    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableField(exist = false)
    public static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @TableField(exist = false)
    public static final String DEFAULT_TIMEZONE = "GMT+8";

    /**
     * 返回对象的关联模型
     */
    @TableField(exist = false)
    private List<RelativeModel> relatives;
    public List<RelativeModel> getRelatives() {
        Field[] fields = this.getClass().getDeclaredFields();
        List<RelativeModel> result = new ArrayList<>();
        for (Field field:
             fields) {
            TableRelative tableRelative = field.getAnnotation(TableRelative.class);
            if (tableRelative != null ) {
                RelativeModel relativeModel = new RelativeModel();
                relativeModel.setId(tableRelative.id());
                relativeModel.setForeignId(tableRelative.foreignId());
                Class<? extends BaseEntity> clz = (Class<? extends BaseEntity>) field.getType();

                if (BaseEntity.class.isAssignableFrom(clz)) {
                    relativeModel.setEntityClass(clz);
                    result.add(relativeModel);
                }
            }
        }

        return result;
    }

    /**
     * logic delete flag
     */
    private Boolean flag;




    /**
     * 表名
     */
    public static String getTableNameByClass(Class<?> clz) {
        TableName name = clz.getDeclaredAnnotation(TableName.class);
        return name == null ? null : name.value();
    }

    public List<Field> entityFields() {
        return entityFieldsByClass(this.getClass(), null);
    }

    /**
     * 获取entity所有数据表字段
     * @param clz entity class
     * @param fields 递归数据表字段
     * @return 数据表字段
     */
    private List<Field> entityFieldsByClass(Class<?> clz, List<Field> fields) {
        if (clz == null) {
            return null;
        }

        if (clz.getSuperclass() != null) {
            fields = entityFieldsByClass(clz.getSuperclass(), fields);
        }

        if (fields == null) {
            fields = new ArrayList<>();
        }

        for (Field field :
                clz.getDeclaredFields()) {
            TableField tableField = field.getAnnotation(TableField.class);
            if (tableField == null || tableField.exist()) {
                fields.add(field);
            }
        }

        return fields;
    }
}
