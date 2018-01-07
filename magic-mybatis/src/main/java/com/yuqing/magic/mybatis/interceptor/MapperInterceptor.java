package com.yuqing.magic.mybatis.interceptor;

import com.yuqing.magic.common.util.ReflectionUtil;
import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import sun.misc.Unsafe;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * @author yuqing
 *
 * @since 1.0.1
 */
@Intercepts({
//        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})/*,
        @Signature(type = Executor.class, method = "insert", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "delete", args = {MappedStatement.class, Object.class})*/
})
public class MapperInterceptor implements Interceptor {

    private static final long PROVIDER_TYPE_OFFSET;

    private static final long PROVIDER_METHOD_OFFSET;

    private static final long SQL_SOURCE_OFFSET;

    public static final String SQL_SUFFIX = "Sql";

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
            replaceSqlSource(ms, providerType, extractArgs(objects));
        }
        String msId = ms.getId();
        return invocation.proceed();
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

    }

    private Object[] extractArgs(Object[] params) {
        if (params == null) {
            return params;
        }

        if (params.length == 1) {
            return null;
        }

        Object[] args = new Object[params.length - 1];
        for (int i = 1; i < params.length; i++) {
            args[i - 1] = params[i];
        }
        return args;
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


    private void replaceSqlSource(MappedStatement mappedStatement, Class providerType, Object args[]) throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Method providerMethod = getProviderMethod(mappedStatement.getSqlSource());

        if (!providerMethod.getName().endsWith(SQL_SUFFIX)) {
            return;
        }
        String name = providerMethod.getName().substring(0, providerMethod.getName().length() - SQL_SUFFIX.length());
        Method m = ReflectionUtil.getMethod(providerType, name, new Class[]{MappedStatement.class, Object[].class}, false);

        if (m == null) {
            throw new IllegalArgumentException(providerType.getClass().getCanonicalName() + "."
                    + name + "(MappedStatement) not exists.");
        }

        SqlNode sqlNode = (SqlNode) m.invoke(providerType.newInstance(), mappedStatement, args);

        DynamicSqlSource dynamicSqlSource = new DynamicSqlSource(mappedStatement.getConfiguration(), sqlNode);

        setSqlSource(mappedStatement, dynamicSqlSource);
    }

    private void setSqlSource(MappedStatement ms, DynamicSqlSource dynamicSqlSource) {
        boolean success = ((Unsafe) ReflectionUtil.getUnsafe()).compareAndSwapObject(ms, SQL_SOURCE_OFFSET, ms.getSqlSource(), dynamicSqlSource);


    }
}
