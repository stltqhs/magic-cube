package com.yuqing.magic.mybatis.util;

import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.proxy.util.ProxyUtil;
import org.apache.commons.beanutils.PropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * mybatis工具类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class MybatisUtil {

    private static final Logger logger = LoggerFactory.getLogger(MybatisUtil.class);

    /**
     * 通过msId获取接口类
     * @param msId
     * @return
     */
    public static Class<?> getMapperClass(String msId) {
        if (msId.indexOf(".") == -1) {
            throw new IllegalArgumentException(msId + " is wrong.");
        }
        String mapperClassStr = msId.substring(0, msId.lastIndexOf("."));
        try {
            return Class.forName(mapperClassStr);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     *
     * @param entity
     * @param <T>
     * @return
     */
    public static <T> T proxyEntityChangeHistory(Object entity) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        T t = (T) ProxyUtil.cglibProxy(entity.getClass(), new EntityChangeHistoryProxy(entity));

        PropertyUtils.copyProperties(t, entity);

        EntityChangeHistoryProxy ent = EntityChangeHistoryProxy.extract(t);

        if (ent != null) {
            ent.getChangeHistory().clear();
        }

        return t;
    }

}
