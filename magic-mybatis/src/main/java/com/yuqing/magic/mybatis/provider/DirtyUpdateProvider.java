package com.yuqing.magic.mybatis.provider;

import com.yuqing.magic.common.util.CommonUtil;
import com.yuqing.magic.common.util.ReflectionUtil;
import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import org.apache.commons.lang.mutable.MutableObject;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.scripting.xmltags.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 脏值更新sql提供类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class DirtyUpdateProvider extends BaseProvider {

    private static final Logger logger = LoggerFactory.getLogger(DirtyUpdateProvider.class);
    public static final String PARAMETER_META_OBJECT = "parameterMetaObject";
    public static final String ORIGINAL_OBJECT = "originalObject";

    private static volatile long ORIGINAL_OBJECT_OFFSET = -1;

    private static volatile long META_OBJECT_OFFSET = -1;

    private static class MyMixedSqlNode extends MixedSqlNode {


        public static final String PARAMETER_NAME = "_parameter";

        public MyMixedSqlNode(List<SqlNode> contents) {
            super(contents);
        }

        @Override
        public boolean apply(DynamicContext context) {
            MutableObject oldValue = new MutableObject();
            MutableObject newValue = new MutableObject();

            replaceParameter(context, oldValue, newValue);

            boolean a = super.apply(context);

            resetParameter(context, oldValue, newValue);

            return a;
        }

        private void resetParameter(DynamicContext context,
                                      MutableObject oldValue,
                                      MutableObject newValue) {
            try {
                doResetParameter(context, oldValue, newValue);
            } catch (IllegalAccessException e) {
                logger.error("", e);
            }
        }

        private void doResetParameter(DynamicContext context,
                                    MutableObject oldValue,
                                    MutableObject newValue) throws IllegalAccessException {
            if (logger.isDebugEnabled()) {
                logger.debug("reset parameter from {} to {}", newValue.getValue(), oldValue.getValue());
            }
            Map<String, Object> bindings = context.getBindings();
            if (CommonUtil.isNullOrEmpty(bindings)) {
                return;
            }

            if (oldValue.getValue() != null) {
                bindings.put(PARAMETER_NAME, oldValue.getValue());
            }

            if (oldValue.getValue() != null && newValue.getValue() != null) {
                replaceMetaObject(bindings, newValue.getValue(), oldValue.getValue());
            }
        }

        private void replaceParameter(DynamicContext context,
                                      MutableObject oldValue,
                                      MutableObject newValue) {
            try {
                doReplaceParameter(context, oldValue, newValue);
            } catch (IllegalAccessException e) {
                logger.error("", e);
            } catch (InstantiationException e) {
                logger.error("", e);
            }
        }

        private void doReplaceParameter(DynamicContext context,
                                        MutableObject oldValue,
                                        MutableObject newValue) throws IllegalAccessException, InstantiationException {
            Map<String, Object> bindings = context.getBindings();
            if (CommonUtil.isNullOrEmpty(bindings)) {
                return;
            }

            Object parameter = bindings.get(PARAMETER_NAME);

            if (parameter == null) {
                return;
            }

            oldValue.setValue(parameter);

            EntityChangeHistoryProxy proxy = EntityChangeHistoryProxy.extract(parameter);

            if (proxy == null) {
                throw new IllegalArgumentException(parameter.getClass().getCanonicalName() + " is not a valid Class for DirtySelective()");
            }

            Object newParameter = proxy.getTarget().getClass().newInstance();

            List<Field> idFields = PersistenceUtil.getIdFields(proxy.getTarget().getClass());

            if (CommonUtil.isNullOrEmpty(idFields)) {
                throw new IllegalArgumentException("Can not find field with @Id for " + proxy.getTarget().getClass());
            }

            for (Field field : idFields) {
                field.setAccessible(true);
                field.set(newParameter, ReflectionUtil.getFieldValue(parameter, field));
            }

            Iterator<Map.Entry<Field, List<?>>> iterator = proxy.getChangeHistory().entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Field, List<?>> item = iterator.next();
                item.getKey().setAccessible(true);
                item.getKey().set(newParameter, CommonUtil.getLast(item.getValue()));
            }

            if (logger.isDebugEnabled()) {
                logger.debug("set parameter from {} to {}", parameter, newParameter);
            }
            bindings.put(PARAMETER_NAME, newParameter);
            newValue.setValue(newParameter);

            replaceMetaObject(bindings, parameter, newParameter);
        }
    }

    private static void replaceMetaObject(Map<String, Object> bindings, Object oldParameter, Object newParameter) throws IllegalAccessException {

        if (META_OBJECT_OFFSET <= 0) {
            Field field = ReflectionUtil.getField(bindings.getClass(), PARAMETER_META_OBJECT, false);
            META_OBJECT_OFFSET = ReflectionUtil.getOffset(bindings.getClass(), field);
        }

        if (META_OBJECT_OFFSET > 0) {
            MetaObject metaObject = (MetaObject) ((Unsafe) ReflectionUtil.getUnsafe()).getObject(bindings, META_OBJECT_OFFSET);

            if (ORIGINAL_OBJECT_OFFSET <= 0) {
                Field field = ReflectionUtil.getField(MetaObject.class, ORIGINAL_OBJECT, true);
                if (field != null) {
                    ORIGINAL_OBJECT_OFFSET = ReflectionUtil.getOffset(bindings.getClass(), field);
                }
            }

            if (ORIGINAL_OBJECT_OFFSET > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug("set meta object from {} to {}", oldParameter, newParameter);
                }
                ((Unsafe) ReflectionUtil.getUnsafe()).compareAndSwapObject(metaObject, ORIGINAL_OBJECT_OFFSET, oldParameter, newParameter);
            }
        }

    }

    public String updateByPrimaryKeyDirtySelectiveSql(Object record) {
        return "updateByPrimaryKeyDirtySelective";
    }

    public SqlNode updateByPrimaryKeyDirtySelective(MappedStatement ms) {
        Class entityClass = getEntityClass(ms);

        if (logger.isDebugEnabled()) {
            logger.debug("create SqlNode for {}", ms.getId());
        }

        List<SqlNode> sqlNodeList = new LinkedList<>();

        sqlNodeList.add(new StaticTextSqlNode("UPDATE " + PersistenceUtil.getTableName(entityClass)));

        appendSetSqlNode(sqlNodeList, entityClass, ms);

        appendWhereSqlNode(sqlNodeList, entityClass, ms);

        return new MyMixedSqlNode(sqlNodeList);
    }

    private void appendWhereSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                    MappedStatement ms) {

        List<SqlNode> whereNodes = new LinkedList<>();
        List<Field> fields = PersistenceUtil.getIdFields(clazz);

        if (!CommonUtil.isNullOrEmpty(fields)) {
            String p = "";
            for (Field field : fields) {
                String column = PersistenceUtil.getColumnName(clazz, field);
                whereNodes.add(new StaticTextSqlNode(p + column + " = #{" + field.getName() + "}"));

                p = " AND ";
            }
        }

        sqlNodes.add(new WhereSqlNode(ms.getConfiguration(), new MixedSqlNode(whereNodes)));
    }

    private void appendSetSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                  MappedStatement ms) {

        List<SqlNode> ifNodes = new LinkedList<>();

        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            Id id = field.getAnnotation(Id.class);
            if (id != null) {
                continue;
            }
            StaticTextSqlNode columnNode = new StaticTextSqlNode(
                    PersistenceUtil.getColumnName(clazz, field) + " = #{" + field.getName() + "}, ");

            if (field.getType().equals(String.class)) {
                ifNodes.add(new IfSqlNode(columnNode, field.getName() + " != null and " + field.getName() + ".trim().length() != 0"));
            } else {
                ifNodes.add(new IfSqlNode(columnNode, field.getName() + " != null"));
            }
        }

        sqlNodes.add(new SetSqlNode(ms.getConfiguration(), new MixedSqlNode(ifNodes)));
    }

}
