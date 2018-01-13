package com.yuqing.magic.mybatis.provider;

import com.yuqing.magic.common.util.CommonUtil;
import com.yuqing.magic.common.util.ReflectionUtil;
import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 版本更新的提供类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class VersionUpdateProvider extends BaseProvider {

    private static final Logger logger = LoggerFactory.getLogger(VersionUpdateProvider.class);

    private static class MyMixedSqlNode extends MixedSqlNode {

        private final Configuration configuration;

        private final boolean enableAlternative;

        public MyMixedSqlNode(List<SqlNode> contents) {
            super(contents);
            configuration = null;
            enableAlternative = false;
        }

        public MyMixedSqlNode(List<SqlNode> contents,
                              Configuration configuration,
                              boolean enableAlternative) {
            super(contents);
            this.configuration = configuration;
            this.enableAlternative = enableAlternative;
        }

        @Override
        public boolean apply(DynamicContext context) {
            addExtraParameters(context);
            boolean result = super.apply(context);
            return result;
        }

        private void addExtraParameters(DynamicContext context) {
            try {
                doAddExtraParameters(context);
            } catch (InstantiationException e) {
                logger.error("", e);
            } catch (IllegalAccessException e) {
                logger.error("", e);
            }
        }

        private void doAddExtraParameters(DynamicContext context) throws InstantiationException, IllegalAccessException {
            // versionEnable 表示启用version的字段
            addVersionEnableParameters(context);
            // old 表示旧数据
            addOldParameters(context);
        }

        private Object getEntityParameter(Map<String, Object> bindings) {
            return getParameter(bindings, "entity");
        }

        private Object getParameter(Map<String, Object> bindings, String name) {
            Object parameter = bindings.get("_parameter");
            if (parameter == null) {
                return null;
            }

            if (!(parameter instanceof Map)) {
                return null;
            }

            return ((Map) parameter).get(name);
        }

        private void addVersionEnableParameters(DynamicContext context) {
            Map<String, Object> bindings = context.getBindings();
            Object parameter = getEntityParameter(bindings);

            if (parameter == null) {
                return;
            }

            // versionEnable 表示启用version的字段

            Field[] fields = parameter.getClass().getDeclaredFields();
            Map<String, Boolean> versionEnable = new HashMap<>();

            EntityChangeHistoryProxy proxy = EntityChangeHistoryProxy.extract(parameter);

            if (proxy == null && enableAlternative) {
                throw new IllegalArgumentException(parameter.getClass().getCanonicalName() + " is not a valid Class for DirtySelective()");
            }

            Map<Field, List<?>> changeHistory = proxy != null ? proxy.getChangeHistory() : null;
            for (Field field : fields) {
                if (MybatisUtil.isId(field) || MybatisUtil.isTransient(field)) {
                    continue;
                }

                if (changeHistory != null && changeHistory.containsKey(field)) {
                    versionEnable.put(PersistenceUtil.getColumnName(field), true);
                } else {
                    versionEnable.put(PersistenceUtil.getColumnName(field), false);
                }
            }

            bindings.put("versionEnable", versionEnable);
        }

        private void addOldParameters(DynamicContext context) throws IllegalAccessException, InstantiationException {
            // old 表示旧数据
            Map<String, Object> bindings = context.getBindings();
            Object parameter = getEntityParameter(bindings);

            if (parameter == null) {
                return;
            }

            EntityChangeHistoryProxy proxy = EntityChangeHistoryProxy.extract(parameter);

            if (proxy == null) {
                if (enableAlternative) {
                    throw new IllegalArgumentException(parameter.getClass().getCanonicalName() + " is not a valid Class for DirtySelective()");
                }
                return;
            }

            Object newParameter = proxy.getTarget().getClass().newInstance();

            Iterator<Map.Entry<Field, List<?>>> iterator = proxy.getChangeHistory().entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Field, List<?>> item = iterator.next();
                item.getKey().setAccessible(true);
                item.getKey().set(newParameter, CommonUtil.getFirst(item.getValue()));
            }

            bindings.put("old", newParameter);
        }
    }

    public String updateByPrimaryKeyVersionSelectiveSql(Object record) {
        return "updateByPrimaryKeyVersionSelective";
    }

    public String updateByPrimaryKeyVersionAlternativeSql(Object record) {
        return "updateByPrimaryKeyVersionAlternative";
    }

    public SqlNode updateByPrimaryKeyVersionSelective(MappedStatement ms) {
        Class entityClass = getEntityClass(ms);

        if (logger.isDebugEnabled()) {
            logger.debug("create SqlNode for {}", ms.getId());
        }

        List<SqlNode> sqlNodeList = new LinkedList<>();

        sqlNodeList.add(MybatisUtil.buildUpdateTableSqlNode(entityClass));

        MybatisUtil.appendSelectiveSetSqlNode(sqlNodeList, entityClass, ms, "entity.");

        MybatisUtil.appendPrimaryKeyAndVersionWhereSqlNode(sqlNodeList, entityClass, ms, "old.");

        return new MyMixedSqlNode(sqlNodeList, ms.getConfiguration(), false);
    }

    public SqlNode updateByPrimaryKeyVersionAlternative(MappedStatement ms) {
        List<SqlNode> sqlNodes = new LinkedList<>();

        return new MixedSqlNode(sqlNodes);
    }

}
