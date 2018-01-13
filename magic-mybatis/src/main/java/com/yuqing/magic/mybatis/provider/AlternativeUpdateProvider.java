package com.yuqing.magic.mybatis.provider;

import com.yuqing.magic.common.util.CommonUtil;
import com.yuqing.magic.common.util.ReflectionUtil;
import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.xmltags.*;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 脏值更新sql提供类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class AlternativeUpdateProvider extends BaseProvider {

    private static final Logger logger = LoggerFactory.getLogger(AlternativeUpdateProvider.class);

    private static class MyMixedSqlNode extends MixedSqlNode {

        private Configuration configuration = null;

        public static final String PARAMETER_NAME = "_parameter";

        public MyMixedSqlNode(List<SqlNode> contents) {
            super(contents);
        }

        public MyMixedSqlNode(List<SqlNode> contents, Configuration configuration) {
            super(contents);
            this.configuration = configuration;
        }

        @Override
        public boolean apply(DynamicContext context) {

            Object newValue = copyOf(context);

            if (newValue != null) {
                DynamicContext tmp = new DynamicContext(configuration, newValue);
                boolean a = super.apply(tmp);

                if (a) {
                    context.appendSql(tmp.getSql());
                }

                return a;
            } else {
                return super.apply(context);
            }
        }

        private Object doCopyOf(DynamicContext context) throws IllegalAccessException, InstantiationException {
            Map<String, Object> bindings = context.getBindings();
            if (CommonUtil.isNullOrEmpty(bindings)) {
                return null;
            }

            Object parameter = bindings.get(PARAMETER_NAME);

            if (parameter == null) {
                return null;
            }

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

            return newParameter;
        }

        private Object copyOf(DynamicContext context) {
            try {
                return doCopyOf(context);
            } catch (IllegalAccessException e) {
                logger.error("", e);
            } catch (InstantiationException e) {
                logger.error("", e);
            }

            return null;
        }
    }

    public String updateByPrimaryKeyAlternativeSql(Object record) {
        return "updateByPrimaryKeyAlternative";
    }

    public SqlNode updateByPrimaryKeyAlternative(MappedStatement ms) {
        Class entityClass = getEntityClass(ms);

        if (logger.isDebugEnabled()) {
            logger.debug("create SqlNode for {}", ms.getId());
        }

        List<SqlNode> sqlNodeList = new LinkedList<>();

        sqlNodeList.add(new StaticTextSqlNode("UPDATE " + PersistenceUtil.getTableName(entityClass)));

        appendSetSqlNode(sqlNodeList, entityClass, ms);

        appendWhereSqlNode(sqlNodeList, entityClass, ms);

        return new MyMixedSqlNode(sqlNodeList, ms.getConfiguration());
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
