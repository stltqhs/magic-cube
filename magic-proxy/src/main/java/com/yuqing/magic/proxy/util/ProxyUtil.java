package com.yuqing.magic.proxy.util;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;

/**
 * 代理通用工具类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class ProxyUtil {

    public static Object cglibProxy(Class supperClass, Callback callback) {
        Enhancer e = new Enhancer();

        e.setSuperclass(supperClass);
        e.setCallback(callback);

        Object bean = e.create();

        return bean;
    }

}
