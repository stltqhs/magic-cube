package com.yuqing.magic.mybatis.provider;

import com.yuqing.magic.common.util.CommonUtil;
import com.yuqing.magic.common.util.ReflectionUtil;
import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
            } catch (NoSuchMethodException e) {
                logger.error("", e);
            } catch (InvocationTargetException e) {
                logger.error("", e);
            }
        }

        private void doAddExtraParameters(DynamicContext context) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
            // versionEnable 表示启用version的字段
            addVersionEnableParameters(context);
            // old 表示旧数据
            addOldParameters(context);
        }

        private Object getEntityParameter(Map<String, Object> bindings) {
            return getParameter(bindings, "entity");
        }

        private String getVersionString(Map<String, Object> bindings) {
            Object object = getParameter(bindings, "version");
            if (object instanceof String) {
                return (String) object;
            }
            return null;
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

        private Map<String, Boolean> buildExplicitVersions(Map<String, Object> bindings) {
            String text = getVersionString(bindings);

            Map<String, Boolean> explicitVersions = new HashMap<>();

            if (StringUtils.isNotBlank(text)) {
                String[] parts = text.split(",");
                for (String p : parts) {
                    if (p.startsWith("-")) {
                        explicitVersions.put(p.substring(1), false);
                    } else if (p.startsWith("+")) {
                        explicitVersions.put(p.substring(1), true);
                    } else {
                        explicitVersions.put(p, true);
                    }
                }
            }

            return explicitVersions;
        }

        private boolean isOnlyVersions(Map<String, Object> bindings) {
            String text = getVersionString(bindings);
            if (StringUtils.isBlank(text)) {
                return false;
            }

            if (text.indexOf("-") == -1
                    && text.indexOf("+") == -1) {
                return true;
            }

            return false;
        }

        private boolean isEnable(Map<String, Boolean> explicitVersions, String name) {
            Boolean t = explicitVersions.get(name);

            return t != null && t;
        }

        private boolean isMaybeEnable(Map<String, Boolean> explicitVersions, String name) {
            boolean e = isEnable(explicitVersions, name);
            if (e) {
                return e;
            }

            return !explicitVersions.containsKey(name);
        }

        private void addVersionEnableParameters(DynamicContext context) {
            Map<String, Object> bindings = context.getBindings();
            Object parameter = getEntityParameter(bindings);

            Map<String, Boolean> versionEnable = new HashMap<>();
            Map<String, Boolean> explicitVersions = buildExplicitVersions(bindings);
            boolean isOnly = isOnlyVersions(bindings);

            if (parameter != null) {
                // versionEnable 表示启用version的字段
                EntityChangeHistoryProxy proxy = EntityChangeHistoryProxy.extract(parameter);

                if (proxy == null && enableAlternative) {
                    throw new IllegalArgumentException(parameter.getClass().getCanonicalName() + " is not a valid Class for DirtySelective()");
                }

                if (proxy != null) {
                    Field[] fields = proxy.getTarget().getClass().getDeclaredFields();
                    Map<Field, List<?>> changeHistory = proxy != null ? proxy.getChangeHistory() : null;
                    for (Field field : fields) {
                        if (MybatisUtil.isId(field) || MybatisUtil.isTransient(field)) {
                            continue;
                        }
                        boolean enable;
                        if (isOnly) {
                            if (explicitVersions.containsKey(field.getName())) {
                                enable = true;
                            } else {
                                enable = false;
                            }
                        } else {
                            if (changeHistory != null
                                    && changeHistory.containsKey(field)
                                    && isMaybeEnable(explicitVersions, field.getName())) {
                                enable = true;
                            } else if (isEnable(explicitVersions, field.getName())) {
                                enable = true;
                            } else {
                                enable = false;
                            }
                        }

                        versionEnable.put(PersistenceUtil.getColumnName(field), enable);
                    }
                }
            }

            bindings.put("versionEnable", versionEnable);
        }

        private void addOldParameters(DynamicContext context) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
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
                bindings.put("old", new Object());
                return;
            }

            Object newParameter = proxy.getTarget().getClass().newInstance();

            PropertyUtils.copyProperties(newParameter, parameter);

            Iterator<Map.Entry<Field, List<?>>> iterator = proxy.getChangeHistory().entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<Field, List<?>> item = iterator.next();
                item.getKey().setAccessible(true);
                item.getKey().set(newParameter, CommonUtil.get(item.getValue(), 0));
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
