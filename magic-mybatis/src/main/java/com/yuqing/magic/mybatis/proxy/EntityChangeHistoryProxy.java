package com.yuqing.magic.mybatis.proxy;

import com.yuqing.magic.common.util.ReflectionUtil;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Id;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 记录对象字段的变更历史
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class EntityChangeHistoryProxy implements MethodInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(EntityChangeHistoryProxy.class);

    public static final String SET = "set";

    @Transient
    private Object target;

    @Transient
    private Map<Field, List<?>> changeHistory = new ConcurrentHashMap<>();

    public EntityChangeHistoryProxy(Object target) {
        this.target = target;
    }

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (isModify(method) && args.length == 1) {
            Field field = getField(obj, method);

            if (field != null) {
                boolean record = true;
                // 主键不需要记录
                Id id = field.getAnnotation(Id.class);
                if (id != null) {
                    record = false;
                }

                // @Transient标记的字段不需要记录
                if (record) {
                    Transient transientAnno = field.getAnnotation(Transient.class);
                    if (transientAnno != null) {
                        record  = false;
                    }
                }
                // 记录
                if (record) {
                    List his = changeHistory.get(field);
                    if (his == null) {
                        his = new ArrayList<>(3);

                        Object fieldValue = getFieldValue(obj, method);
                        his.add(fieldValue);

                        changeHistory.put(field, his);
                    }

                    his.add(args[0]);
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Can not infer field name from " + obj.getClass() + "." + method.getName());
                }
            }
        }
        return methodProxy.invokeSuper(obj, args);
    }

    private boolean isModify(Method method) {
        return method.getName().startsWith(SET);
    }

    private Object getFieldValue(Object obj, Method setMethod) {
        Method get = obtainGetMethod(obj, setMethod);
        if (get != null) {
            return ReflectionUtil.getMethodValue(obj, get, null);
        }

        Field field = getField(obj, setMethod);

        if (field != null) {
            return ReflectionUtil.getFieldValue(obj, field);
        }

        throw new IllegalArgumentException("Can not infer field for " + setMethod.getName() + " method");
    }

    private Field getField(Object obj, Method method) {
        String name = method.getName();
        name = name.substring(SET.length());
        if (name.length() > 1) {
            name = name.substring(0, 1).toLowerCase() + name.substring(1);
        } else {
            name = name.substring(0, 1).toLowerCase();
        }

        Field field = ReflectionUtil.getField(obj.getClass(), name, true);

        return field;
    }

    private Method obtainGetMethod(Object obj, Method setMethod) {
        String name = setMethod.getName();
        name = "g" + name.substring(1);

        return ReflectionUtil.getMethod(obj.getClass(), name, null, true);
    }

    public static EntityChangeHistoryProxy extract(Object t) {
        Field a = ReflectionUtil.getField(t.getClass(), "CGLIB$CALLBACK_0", false);

        if (a == null) {
            return null;
        }

        Object b = ReflectionUtil.getFieldValue(t, a);

        if (b instanceof EntityChangeHistoryProxy) {
            return (EntityChangeHistoryProxy) b;
        }

        return null;
    }

    public Map<Field, List<?>> getChangeHistory() {
        return changeHistory;
    }

    public Object getTarget() {
        return target;
    }
}
