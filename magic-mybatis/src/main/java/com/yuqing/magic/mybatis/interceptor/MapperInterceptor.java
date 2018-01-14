package com.yuqing.magic.mybatis.interceptor;

import com.sun.javafx.collections.MappingChange;
import com.yuqing.magic.common.util.CommonUtil;
import com.yuqing.magic.common.util.NumberUtil;
import com.yuqing.magic.common.util.ReflectionUtil;
import com.yuqing.magic.mybatis.annotation.EnableAlternative;
import com.yuqing.magic.mybatis.bean.ModifiableBoundSql;
import com.yuqing.magic.mybatis.bean.SqlModifier;
import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.builder.annotation.ProviderSqlSource;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.BaseExecutor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.BaseStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * 处理基于{@link BaseProvider}的Mapper和工具类设置
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = StatementHandler.class, method = "update", args = {Statement.class})
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

    /**
     * 代理结果集的控制，默认为当结果集中的实体存在@ProxyChangeHistory注解时才代理
     */
    private String resultProxy = PROXY_WITH_DECLARE_ANNOTATION;

    /**
     * 代理结果集的控制，默认为当返回的结果集数量为1时才代理
     */
    private int resultSizeProxy = 1;

    static {
        PROVIDER_TYPE_OFFSET = ReflectionUtil.getOffset(ProviderSqlSource.class, "providerType");
        PROVIDER_METHOD_OFFSET = ReflectionUtil.getOffset(ProviderSqlSource.class, "providerMethod");
        SQL_SOURCE_OFFSET = ReflectionUtil.getOffset(MappedStatement.class, "sqlSource");
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if (invocation.getTarget() instanceof Executor) {
            return interceptExecutor(invocation);
        } else if (invocation.getTarget() instanceof StatementHandler) {
            return interceptStatementHandler(invocation);
        }

        return invocation.proceed();
    }

    private Object interceptExecutor(Invocation invocation) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Object[] objects = invocation.getArgs();
        MappedStatement ms = (MappedStatement) objects[0];
        Class<?> providerType = getProviderType(ms.getSqlSource());
        if (providerType != null && BaseProvider.class.isAssignableFrom(providerType)) {
            replaceSqlSource(ms, providerType);
        }

        if (canModifySql()) {
            modifySql(invocation);
        }

        if (MybatisUtil.hasJustReturn()) {
            Object jt = MybatisUtil.getJustReturn();
            MybatisUtil.clearJustReturn();
            return jt;
        }
        Object result = invocation.proceed();
        if (SqlCommandType.SELECT.equals(ms.getSqlCommandType())) {
            result = replaceResult(result);
        }

        return result;
    }

    private Object interceptStatementHandler(Invocation invocation) throws InvocationTargetException, IllegalAccessException {
        String method = invocation.getMethod().getName();
        if (method.equals("query")) {

        } else if (method.equals("update")) {
            if (canModifySql()) {
                modifySql(invocation.getArgs(), (StatementHandler) invocation.getTarget());
            }
        }

        return invocation.proceed();
    }

    private boolean canModifySql() {
        return !CommonUtil.isNullOrEmpty(MybatisUtil.getSqlModifiers());
    }

    private void modifySql(Object[] args, StatementHandler statementHandler) {
        List<SqlModifier> sqlModifierList = MybatisUtil.getSqlModifiers();
        MybatisUtil.clearSqlModifiers();

        if (!(statementHandler instanceof StatementHandler)) {
            return;
        }

        BaseStatementHandler baseStatementHandler = getBaseStatementHandler((StatementHandler) statementHandler);

        if (baseStatementHandler == null) {
            return;
        }

        Field configurationField = ReflectionUtil.getField(BaseStatementHandler.class, "configuration", false);

        if (configurationField == null) {
            return;
        }

        Configuration configuration = (Configuration) ReflectionUtil.getFieldValue(baseStatementHandler, configurationField);

        if (configuration == null) {
            return;
        }

        BoundSql boundSql = MybatisUtil.createModifiableBoundSql(configuration
                ,
                baseStatementHandler.getBoundSql());


        doModifySql((ModifiableBoundSql) boundSql, sqlModifierList);

        ReflectionUtil.setFieldValue(baseStatementHandler, "boundSql", boundSql);

        Executor executor = (Executor) ReflectionUtil.getFieldValue(baseStatementHandler, "executor");
        MappedStatement mappedStatement = (MappedStatement) ReflectionUtil.getFieldValue(baseStatementHandler, "mappedStatement");

        if (executor instanceof BaseExecutor) {

            BaseExecutor baseExecutor = (BaseExecutor) executor;

            StatementHandler sh = configuration.newStatementHandler(executor,
                    mappedStatement /*MappedStatement*/,
                    boundSql.getParameterObject() /*parameterObject*/,
                    RowBounds.DEFAULT /*rowBounds*/,
                    null /**/,
                    boundSql);


            Statement stmt;
            Connection connection = (Connection) ReflectionUtil.getMethodValue(executor,
                    "getConnection",
                    new Object[]{mappedStatement.getStatementLog()},
                    new Class[]{Log.class});
            try {
                stmt = sh.prepare(connection, baseExecutor.getTransaction().getTimeout());
                sh.parameterize(stmt);

                args[0] = stmt;
            } catch (SQLException e) {
                logger.error("", e);
                return;
            }
        }

    }

    private BaseStatementHandler getBaseStatementHandler(StatementHandler statementHandler) {
        if (statementHandler instanceof BaseStatementHandler) {
            return (BaseStatementHandler) statementHandler;
        }

        Field delegate = ReflectionUtil.getField(statementHandler.getClass(), "delegate", false);

        if (delegate != null) {
            Object delegateObject = ReflectionUtil.getFieldValue(statementHandler, delegate);

            if (delegateObject instanceof StatementHandler) {
                return getBaseStatementHandler((StatementHandler) delegateObject);
            }
        }

        return null;
    }

    private void modifySql(Invocation invocation) {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        Executor executor = (Executor) invocation.getTarget();
        CacheKey cacheKey;
        BoundSql boundSql = null;
        boolean modify = false;

        if (logger.isDebugEnabled()) {
            logger.debug("modify {} sql", ms.getId());
        }

        if (args.length == 4) {
            boundSql = MybatisUtil.createModifiableBoundSql(ms.getConfiguration(), ms.getBoundSql(parameter));
            cacheKey = executor.createCacheKey(ms, parameter, (RowBounds) args[2], boundSql);
            modify = true;
        } else if (args.length == 6){
            boundSql = (BoundSql) args[5];
            if (!(boundSql instanceof ModifiableBoundSql)) {
                boundSql = MybatisUtil.createModifiableBoundSql(ms.getConfiguration(), boundSql);
                args[5] = boundSql;
                modify = true;
            }
        }

        if (modify && boundSql instanceof ModifiableBoundSql) {
            List<SqlModifier> sqlModifierList = MybatisUtil.getSqlModifiers();
            MybatisUtil.clearSqlModifiers();
            doModifySql((ModifiableBoundSql) boundSql, sqlModifierList);
        }
    }

    private void doModifySql(ModifiableBoundSql boundSql, List<SqlModifier> sqlModifiers) {
        if (CommonUtil.isNullOrEmpty(sqlModifiers)) {
            if (logger.isDebugEnabled()) {
                logger.debug("There's no SqlModifier exists in this Thread.");
            }
            return;
        }

        String sql = boundSql.getSql();

        for (SqlModifier sqlModifier : sqlModifiers) {
            if (Objects.equals(SqlModifier.HEADER, sqlModifier.getColumn())) {
                sql = sqlModifier.getModification() + sql;
            } else if (Objects.equals(SqlModifier.TAILING, sqlModifier.getColumn())) {
                sql = sql + sqlModifier.getModification();
            } else {
                sql = replaceColumn(sql, sqlModifier);
            }
        }

        boundSql.setSql(sql);
    }

    private String replaceColumn(String sql , SqlModifier sqlModifier) {
        int step = 0;
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (step == 0) { // find update
                if (c == 'u' || c == 'U') {
                    buffer.append(c);
                    int j = i;
                    i++;
                    for (;; i++) {
                        c = sql.charAt(i);
                        if (c == ' ' || c == '\n' || c == '`') {
                            break;
                        }
                        buffer.append(c);
                    }

                    if (buffer.toString().equalsIgnoreCase("update")) {
                        step = 1;
                    } else {
                        i = j;
                    }
                    buffer.setLength(0);
                }
            } else if (step == 1) { // find table
                if (!(c == ' ' || c == '\n' || c == '`')) {
                    i++;
                    for(;; i++) {
                        c = sql.charAt(i);
                        if (c == ' ' || c == '\n' || c == '`') {
                            step = 2;
                            break;
                        }
                    }
                }
            } else if (step == 2) { // find set
                if (c == 's' || c == 'S') {
                    buffer.append(c);
                    int j = i;
                    i++;
                    for (;; i++) {
                        c = sql.charAt(i);
                        if (c == ' ' || c == '\n' || c == '`') {
                            break;
                        }
                        buffer.append(c);
                    }

                    if (buffer.toString().equalsIgnoreCase("set")) {
                        step = 3;
                    } else {
                        i = j;
                    }
                    buffer.setLength(0);
                }
            } else if (step == 3) { // find column
                sql = replaceColumn(sql, sqlModifier, i);
                break;
            }
        }
        return sql;
    }

    private String replaceColumn(String sql, SqlModifier sqlModifier, int start) {
        StringBuilder buffer = new StringBuilder();
        int step = 0; // find column name
        int j = -1, k = 0;
        int i = start;
        for (; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (step == 0 && c != ' ' && c != '`' && c != '\n' && c != ',') {
                buffer.append(c);
                if (j == -1) {
                    j = i;
                }
            } else if (step == 1 && c == '=') {
                step = 2;
                k = i + 1;
                if (buffer.toString().equalsIgnoreCase(sqlModifier.getColumn())) {
                    step = 3;
                    break;
                }
            } else if (step == 2){
                step = 0;
                buffer.setLength(0);
                j = -1;
            } else if (step == 0) {
                step = 1; // find =
                i--;
            }
        }
        if (step == 3) {
            buffer.setLength(0);
            buffer.append(sql.substring(0, j));
            buffer.append(sqlModifier.getModification());
            buffer.append(sql.substring(k));

            return buffer.toString();
        }
        return sql;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof Executor
                || target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        } else {
            return target;
        }
    }

    @Override
    public void setProperties(Properties properties) {
        setResultProxyProperty(properties);

        setResultSizeProxyProperty(properties);
    }

    private void setResultSizeProxyProperty(Properties properties) {
        String resultSize = properties.getProperty("resultSizeProxy");
        boolean rsEmpty = StringUtils.isBlank(resultSize);
        boolean rsNumber = NumberUtil.isNumber(resultSize);
        if (!rsEmpty && rsNumber) {
            int rs = NumberUtil.safeParseInteger(resultSize, 1);
            if (logger.isDebugEnabled()) {
                logger.debug("set resultSizeProxy to {}", rs);
            }
            resultSizeProxy = rs;
        } else if (!rsEmpty && !rsNumber) {
            logger.warn("try to set resultSizeProxy to {} but value is wrong.", resultSize);
        }
    }

    private void setResultProxyProperty(Properties properties) {
        String rp = properties.getProperty("resultProxy");

        if (PROXY_ALL.equals(rp)
                || PROXY_WITH_DECLARE_ANNOTATION.equals(rp)
                || PROXY_DISABLE.equals(rp)) {
            if (logger.isDebugEnabled()) {
                logger.debug("set resultProxy to {}", rp);
            }
            resultProxy = rp;
        } else {
            logger.warn("try to set resultProxy to {} but value is wrong.", rp);
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

        EnableAlternative proxyChangeHistory = result.getClass().getAnnotation(EnableAlternative.class);

        if (logger.isDebugEnabled()) {
            logger.debug("{} find EnableAlternative annotation for {}",
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
        if (result == null) {
            return result;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Encounter an Collection.Iterates its item.size={}", result.size());
        }
        if (result.size() > resultSizeProxy) {
            if (logger.isDebugEnabled()) {
                logger.debug("Collection.size={} is bigger than resultSizeProxy={},ignore this result.",
                        result.size(), resultSizeProxy);
            }
            return result;
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
