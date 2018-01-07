package com.yuqing.magic.mybatis.interceptor;

import com.yuqing.magic.common.util.ReflectionUtil;
import com.yuqing.magic.mybatis.annotation.ProxyChangeHistory;
import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

/**
 * @author yuqing
 *
 * @since 1.0.1
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})/*,
        @Signature(type = Executor.class, method = "insert", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "delete", args = {MappedStatement.class, Object.class})*/
})
public class MapperInterceptor implements Interceptor {

    private static final Logger logger = LoggerFactory.getLogger(MapperInterceptor.class);

    public static final String PROXY_WITH_DECLARE_ANNOTATION = "annotation";

    public static final String PROXY_ALL = "all";

    public static final String PROXY_DISABLE = "disable";

    private static final long PROVIDER_TYPE_OFFSET;

    private static final long PROVIDER_METHOD_OFFSET;

    private static final long SQL_SOURCE_OFFSET;

    public static final String SQL_SUFFIX = "Sql";

    private String resultProxy = PROXY_WITH_DECLARE_ANNOTATION;

    static {
        PROVIDER_TYPE_OFFSET = ReflectionUtil.getOffset(ProviderSqlSource.class, "providerType");
        PROVIDER_METHOD_OFFSET = ReflectionUtil.getOffset(ProviderSqlSource.class, "providerMethod");
        SQL_SOURCE_OFFSET = ReflectionUtil.getOffset(MappedStatement.class, "sqlSource");
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] objects = invocation.getArgs();
        MappedStatement ms = (MappedStatement) objects[0];
        Class<?> providerType = getProviderType(ms.getSqlSource());
        if (providerType != null && BaseProvider.class.isAssignableFrom(providerType)) {
            replaceSqlSource(ms, providerType);
        }
        Object result = invocation.proceed();
        if (SqlCommandType.SELECT.equals(ms.getSqlCommandType())) {
            result = replaceResult(result);
        }

        return result;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {
        String rp = properties.getProperty("resultProxy");

        if (PROXY_ALL.equals(rp)
                || PROXY_WITH_DECLARE_ANNOTATION.equals(rp)
                || PROXY_DISABLE.equals(rp)) {
            if (logger.isDebugEnabled()) {
                logger.debug("set resultProxy to {}", rp);
            }
            resultProxy = rp;
        } else {
            logger.warn("try to set resultProxy but value is wrong.");
        }
    }

    private Class getProviderType(SqlSource sqlSource) {
        if (sqlSource instanceof ProviderSqlSource) {
            if (PROVIDER_TYPE_OFFSET > 0 && ReflectionUtil.getUnsafe() != null) {
                return (Class) ((Unsafe) ReflectionUtil.getUnsafe()).getObject(sqlSource, PROVIDER_TYPE_OFFSET);
            }
        }

        return null;
    }
    private Method getProviderMethod(SqlSource sqlSource) {
        if (sqlSource instanceof ProviderSqlSource) {
            if (PROVIDER_METHOD_OFFSET > 0 && ReflectionUtil.getUnsafe() != null) {
                return (Method) ((Unsafe) ReflectionUtil.getUnsafe()).getObject(sqlSource, PROVIDER_METHOD_OFFSET);
            }
        }

        return null;
    }


    private void replaceSqlSource(MappedStatement mappedStatement, Class providerType) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Method providerMethod = getProviderMethod(mappedStatement.getSqlSource());

        if (!providerMethod.getName().endsWith(SQL_SUFFIX)) {
            return;
        }
        String name = providerMethod.getName().substring(0, providerMethod.getName().length() - SQL_SUFFIX.length());
        Method m = ReflectionUtil.getMethod(providerType, name, new Class[]{MappedStatement.class}, false);

        if (m == null) {
            throw new IllegalArgumentException(providerType.getClass().getCanonicalName() + "."
                    + name + "(MappedStatement) not exists.");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("begin to replace sql source for " + providerType.getCanonicalName() + "." + m);
        }

        SqlNode sqlNode = (SqlNode) m.invoke(providerType.newInstance(), mappedStatement);

        DynamicSqlSource dynamicSqlSource = new DynamicSqlSource(mappedStatement.getConfiguration(), sqlNode);

        setSqlSource(mappedStatement, dynamicSqlSource);

        if (logger.isDebugEnabled()) {
            logger.debug("finished to replace sql source.");
        }
    }

    private void setSqlSource(MappedStatement ms, DynamicSqlSource dynamicSqlSource) {
        boolean success = ((Unsafe) ReflectionUtil.getUnsafe()).compareAndSwapObject(ms, SQL_SOURCE_OFFSET, ms.getSqlSource(), dynamicSqlSource);

        if (logger.isDebugEnabled()) {
            logger.debug("replace {}", success ? "success" : "failed");
        }
    }

    private Object replaceResult(Object result) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (result == null) {
            return result;
        }
        if (PROXY_DISABLE.equals(resultProxy)) { // 不支持代理
            return result;
        }

        EntityChangeHistoryProxy historyProxy = EntityChangeHistoryProxy.extract(result);

        if (historyProxy != null) {
            historyProxy.getChangeHistory().clear();
            if (logger.isDebugEnabled()) {
                logger.debug("result is EntityChangeHistoryProxy,clear its history.");
            }
            return result;
        }

        boolean isFinal = Modifier.isFinal(result.getClass().getModifiers());

        if (isFinal) {
            logger.debug(result.getClass().getCanonicalName() + " is final class, Can not proxy it.");
            return result;
        }

        ProxyChangeHistory proxyChangeHistory = result.getClass().getAnnotation(ProxyChangeHistory.class);

        if (logger.isDebugEnabled()) {
            logger.debug("{} find ProxyChangeHistory annotation for {}",
                    proxyChangeHistory != null ? "Has" : "Not", result.getClass());
        }

        if (proxyChangeHistory != null) {
            return proxyResult(result);
        }

        if (PROXY_ALL.equals(resultProxy)) {
            return proxyResult(result);
        }

        if (result instanceof Collection) {
            return proxyResultForCollection((Collection) result);
        }

        return result;
    }

    private Object proxyResult(Object result) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        if (result instanceof Collection) {
            result = proxyResultForCollection((Collection) result);
        } else {
            return MybatisUtil.proxyEntityChangeHistory(result);
        }

        return result;
    }

    private Object proxyResultForCollection(Collection result) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        if (logger.isDebugEnabled()) {
            logger.debug("Encounter an Collection.Iterates its item.{}", result);
        }
        Iterator<Object> iterator = result.iterator();
        Collection backup = (Collection) result.getClass().newInstance();
        while (iterator.hasNext()) {
            Object item = replaceResult(iterator.next());
            backup.add(item);
        }
        result = backup;
        return result;
    }
}
