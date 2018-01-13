package com.yuqing.magic.mybatis.util;

import com.yuqing.magic.common.util.CommonUtil;
import com.yuqing.magic.mybatis.bean.SqlModifier;
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

    private static final ThreadLocal<Object> JUST_RETURN_LOCAL = new ThreadLocal<>();

    private static final ThreadLocal<List<SqlModifier>> SQL_MODIFIERS_LOCAL = new ThreadLocal<>();

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

    public static void appendInsertColumnSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                                 MappedStatement ms, boolean selective) {

        StringBuilder cols = new StringBuilder();
        List<SqlNode> subSqlNodes = new LinkedList<>();

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (MybatisUtil.isTransient(field)) {
                continue;
            }
            if (selective) {
                addCheckIfTest(subSqlNodes, field, new StaticTextSqlNode("," + PersistenceUtil.getColumnName(field)), null);
            } else {
                cols.append("," + PersistenceUtil.getColumnName(field));
            }
        }

        if (!selective) {
            subSqlNodes.add(new StaticTextSqlNode(cols.toString()));
        }

        sqlNodes.add(new TrimSqlNode(ms.getConfiguration(),
                new MixedSqlNode(subSqlNodes), "(", ",", ")", ""));
    }

    public static void appendValuesSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                           MappedStatement ms, String accessPrefix, boolean selective) {
        Field[] fields = clazz.getDeclaredFields();

        List<SqlNode> subSqlNodes = new LinkedList<>();
        StringBuilder cols = new StringBuilder();

        for (Field field : fields) {
            if (MybatisUtil.isTransient(field)) {
                continue;
            }
            if (selective) {
                addCheckIfTest(subSqlNodes, field, new StaticTextSqlNode("," + buildBindFieldText(field, accessPrefix)), null);
            } else {
                cols.append("," + buildBindFieldText(field, accessPrefix));
            }
        }

        if (!selective) {
            subSqlNodes.add(new StaticTextSqlNode(cols.toString()));
        }

        sqlNodes.add(new TrimSqlNode(ms.getConfiguration(),
                new MixedSqlNode(subSqlNodes), "VALUES(", ",", ")", ""));
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
                    " AND " + buildColumnEqualsText(field, accessPrefix)
                            );

            ChooseSqlNode chooseSqlNode = new ChooseSqlNode((List) Arrays.asList(
                    new IfSqlNode(columnNode, getAccessNameWithPrefix(field.getName(), accessPrefix) + " != null")),
                    new StaticTextSqlNode(" AND " + PersistenceUtil.getColumnName(field) + " is null"));

            whereNodes.add(new IfSqlNode(chooseSqlNode, "versionEnable." + field.getName()));
        }

        sqlNodes.add(new WhereSqlNode(ms.getConfiguration(), new MixedSqlNode(whereNodes)));
    }

    private static String buildColumnEqualsText(Field field, String accessPrefix) {
        return PersistenceUtil.getColumnName(field) + " = " + buildBindFieldText(field, accessPrefix);
    }

    private static String buildBindFieldText(Field field, String accessPrefix) {
        return "#{"
                + getAccessNameWithPrefix(field.getName(), accessPrefix) + "}";
    }

    public static boolean isId(Field field) {
        return PersistenceUtil.isId(field);
    }

    public static boolean isTransient(Field field) {
        return PersistenceUtil.isTransient(field);
    }

    private static void populatePrimaryKeyWhereSqlNode(Class clazz, List<SqlNode> whereNodes, String accessPrefix) {
        List<Field> fields = PersistenceUtil.getIdFields(clazz);

        if (!CommonUtil.isNullOrEmpty(fields)) {
            String p = "";
            for (Field field : fields) {
                whereNodes.add(new StaticTextSqlNode(p + buildColumnEqualsText(field, accessPrefix)));

                p = " AND ";
            }
        }
    }

    public static void appendSetSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                                 MappedStatement ms, String accessPrefix,
                                        boolean selective,
                                        boolean trimSet) {
        List<SqlNode> ifNodes = new LinkedList<>();

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (isId(field) || isTransient(field)) {
                continue;
            }
            StaticTextSqlNode columnNode = new StaticTextSqlNode(
                    buildColumnEqualsText(field, accessPrefix) + ",");

            if (selective) {
                addCheckIfTest(ifNodes, field, columnNode, accessPrefix);
            } else {
                ifNodes.add(columnNode);
            }
        }

        SqlNode sn = new SetSqlNode(ms.getConfiguration(), new MixedSqlNode(ifNodes));
        if (trimSet) {
            sn = new TrimSqlNode(ms.getConfiguration(), sn, "", "SET", "", "");
        }
        sqlNodes.add(sn);
    }

    private static void addCheckIfTest(List<SqlNode> ifNodes, Field field, SqlNode sqlNode, String accessPrefix) {
        String test;
        if (field.getType().equals(String.class)) {
            test = buildCheckEmptyIfTest(accessPrefix, field);
        } else {
            test = buildCheckNullIfTest(accessPrefix, field);
        }
        ifNodes.add(new IfSqlNode(sqlNode, test));
    }

    private static String buildCheckNullIfTest(String accessPrefix, Field field) {
        return getFieldNameWithPrefix(field, accessPrefix) + " != null";
    }

    private static String buildCheckEmptyIfTest(String accessPrefix, Field field) {
        return getFieldNameWithPrefix(field, accessPrefix)
                + " != null and "
                + getFieldNameWithPrefix(field, accessPrefix)
                + ".trim().length() != 0";
    }

    public static void appendSelectiveSetSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                  MappedStatement ms, String accessPrefix, boolean trimSet) {

        appendSetSqlNode(sqlNodes, clazz, ms, accessPrefix, true, trimSet);
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

    public static void justReturn(Object sqlReturn) {
        JUST_RETURN_LOCAL.set(sqlReturn);
    }

    public static boolean hasJustReturn() {
        return JUST_RETURN_LOCAL.get() != null;
    }

    public static void clearJustReturn() {
        JUST_RETURN_LOCAL.set(null);
    }

    public static Object getJustReturn() {
        return JUST_RETURN_LOCAL.get();
    }

    public static List<SqlModifier> getSqlModifiers() {
        return SQL_MODIFIERS_LOCAL.get();
    }

    public static void clearSqlModifiers() {
        SQL_MODIFIERS_LOCAL.set(null);
    }

    public static void addSqlModifier(SqlModifier sqlModifier) {
        List<SqlModifier> list = SQL_MODIFIERS_LOCAL.get();
        if (list == null) {
            list = new LinkedList<>();
        }
        list.add(sqlModifier);
        SQL_MODIFIERS_LOCAL.set(list);
    }

    public static void lockForUpdate() {
        addSqlModifier(new SqlModifier(SqlModifier.TAILING, " FOR UPDATE"));
    }

    public static void change(String column, String newColumn) {
        addSqlModifier(new SqlModifier(column, newColumn));
    }
}
