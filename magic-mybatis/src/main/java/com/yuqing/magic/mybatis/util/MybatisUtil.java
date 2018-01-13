package com.yuqing.magic.mybatis.util;

import com.yuqing.magic.common.util.CommonUtil;
import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import com.yuqing.magic.proxy.util.ProxyUtil;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.xmltags.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * mybatis工具类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class MybatisUtil {

    private static final Logger logger = LoggerFactory.getLogger(MybatisUtil.class);

    /**
     * 通过msId获取接口类
     * @param msId
     * @return
     */
    public static Class<?> getMapperClass(String msId) {
        if (msId.indexOf(".") == -1) {
            throw new IllegalArgumentException(msId + " is wrong.");
        }
        String mapperClassStr = msId.substring(0, msId.lastIndexOf("."));
        try {
            return Class.forName(mapperClassStr);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     *
     * @param entity
     * @param <T>
     * @return
     */
    public static <T> T proxyEntityChangeHistory(Object entity) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        T t = (T) ProxyUtil.cglibProxy(entity.getClass(), new EntityChangeHistoryProxy(entity));

        PropertyUtils.copyProperties(t, entity);

        EntityChangeHistoryProxy ent = EntityChangeHistoryProxy.extract(t);

        if (ent != null) {
            ent.getChangeHistory().clear();
        }

        return t;
    }

    public static void appendPrimaryKeyWhereSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                    MappedStatement ms, String accessPrefix) {

        List<SqlNode> whereNodes = new LinkedList<>();

        populatePrimaryKeyWhereSqlNode(clazz, whereNodes, accessPrefix);

        sqlNodes.add(new WhereSqlNode(ms.getConfiguration(), new MixedSqlNode(whereNodes)));
    }

    public static void appendPrimaryKeyAndVersionWhereSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                                    MappedStatement ms, String accessPrefix) {

        List<SqlNode> whereNodes = new LinkedList<>();

        populatePrimaryKeyWhereSqlNode(clazz, whereNodes, "entity.");

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (isId(field) || isTransient(field)) {
                continue;
            }
            StaticTextSqlNode columnNode = new StaticTextSqlNode(
                    " AND " +
                            PersistenceUtil.getColumnName(field) + " = #{"
                            + getAccessNameWithPrefix(field.getName(), accessPrefix) + "}");

            ChooseSqlNode chooseSqlNode = new ChooseSqlNode((List) Arrays.asList(
                    new IfSqlNode(columnNode, getAccessNameWithPrefix(field.getName(), accessPrefix) + " != null")),
                    new StaticTextSqlNode(" AND " + PersistenceUtil.getColumnName(field) + " is null"));

            whereNodes.add(new IfSqlNode(chooseSqlNode, "versionEnable." + field.getName()));
        }

        sqlNodes.add(new WhereSqlNode(ms.getConfiguration(), new MixedSqlNode(whereNodes)));
    }

    public static boolean isId(Field field) {
        Id id = field.getAnnotation(Id.class);
        if (id != null) {
            return true;
        }
        return false;
    }

    public static boolean isTransient(Field field) {
        Transient transientAnno = field.getAnnotation(Transient.class);
        if (transientAnno != null) {
            return true;
        }
        return false;
    }

    private static void populatePrimaryKeyWhereSqlNode(Class clazz, List<SqlNode> whereNodes, String acessPrefix) {
        List<Field> fields = PersistenceUtil.getIdFields(clazz);

        if (!CommonUtil.isNullOrEmpty(fields)) {
            String p = "";
            for (Field field : fields) {
                String column = PersistenceUtil.getColumnName(field);
                whereNodes.add(new StaticTextSqlNode(p + column + " = #{"
                        + getFieldNameWithPrefix(field, acessPrefix) + "}"));

                p = " AND ";
            }
        }
    }

    public static void appendSelectiveSetSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                  MappedStatement ms, String accesPrefix) {

        List<SqlNode> ifNodes = new LinkedList<>();

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (isId(field) || isTransient(field)) {
                continue;
            }
            StaticTextSqlNode columnNode = new StaticTextSqlNode(
                    PersistenceUtil.getColumnName(field) + " = #{"
                            + getFieldNameWithPrefix(field, accesPrefix) + "}, ");

            if (field.getType().equals(String.class)) {
                ifNodes.add(new IfSqlNode(columnNode,
                        getFieldNameWithPrefix(field, accesPrefix)
                                + " != null and "
                                + getFieldNameWithPrefix(field, accesPrefix)
                                + ".trim().length() != 0"));
            } else {
                ifNodes.add(new IfSqlNode(columnNode, getFieldNameWithPrefix(field, accesPrefix) + " != null"));
            }
        }

        sqlNodes.add(new SetSqlNode(ms.getConfiguration(), new MixedSqlNode(ifNodes)));
    }

    private static String getFieldNameWithPrefix(Field field, String prefix) {
        return getAccessNameWithPrefix(field.getName(), prefix);
    }

    public static String getAccessNameWithPrefix(String name, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            return name;
        }

        return prefix + name;
    }

    public static SqlNode buildUpdateTableSqlNode(Class entityClass) {
        return new StaticTextSqlNode("UPDATE " + PersistenceUtil.getTableName(entityClass));
    }

    public static Class getEntityClass(MappedStatement ms) {
        String msId = ms.getId();
        Class<?> mapperClass = getMapperClass(msId);
        Type[] types = mapperClass.getGenericInterfaces();
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                ParameterizedType t = (ParameterizedType) type;
                Class<?> returnType = (Class<?>) t.getActualTypeArguments()[0];
                return returnType;
            }
        }
        return null;
    }

}
