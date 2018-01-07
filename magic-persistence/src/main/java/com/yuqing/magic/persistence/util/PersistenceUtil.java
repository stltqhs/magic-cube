package com.yuqing.magic.persistence.util;

import com.yuqing.magic.common.util.ReflectionUtil;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.util.List;

/**
 * @author yuqing
 *
 * @since 1.0.1
 */
public class PersistenceUtil {

    /**
     * 获取实体的表名
     *
     * @param clazz
     * @return
     */
    public static String getTableName(Class clazz) {
        Table annotation = (Table) clazz.getAnnotation(Table.class);
        if (annotation == null) {
            return clazz.getName();
        }
        return annotation.name();
    }

    public static String getColumnName(Class clazz, String fieldName) {
        Field field = ReflectionUtil.getField(clazz, fieldName, true);
        if (field == null) {
            return fieldName;
        }

        return getColumnName(clazz, field);
    }

    public static String getColumnName(Class clazz, Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null) {
            return column.name();
        }

        return field.getName();
    }

    public static List<Field> getIdFields(Class clazz) {
        return ReflectionUtil.getField(clazz, Id.class);
    }
}