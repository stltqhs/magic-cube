package com.yuqing.magic.proxy.callback;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;

/**
 * @author yuqing
 *
 * @since 1.0.1
 */
public class EntityCallback implements MethodInterceptor {

    private Object target;

    public EntityCallback(Object target) {
        this.target = target;
    }

    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        return methodProxy.invoke(target, objects);
    }
}
