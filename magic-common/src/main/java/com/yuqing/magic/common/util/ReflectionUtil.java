package com.yuqing.magic.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 反射帮助类
 *
 * @author yuqing
 *
 * @see 1.0.1
 */
public class ReflectionUtil {

    private static final Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);

    public static final String SUN_MISC_UNSAFE = "sun.misc.Unsafe";

    /**
     * 获取Unsafe对象
     * @return
     */
    public static Object getUnsafe() {
        try {
            Field field = Class.forName(SUN_MISC_UNSAFE).getDeclaredField("theUnsafe");
            if (field != null) {
                field.setAccessible(true);
                return field.get(null);
            }
        } catch (NoSuchFieldException e) {
            logger.debug("", e);
        } catch (ClassNotFoundException e) {
            logger.debug("", e);
        } catch (IllegalAccessException e) {
            logger.debug("", e);
        }

        return null;
    }

    /**
     * 判断name是否存在
     * @param name
     * @return
     */
    public static boolean hasClass(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            logger.debug(name, e);
        }
        return false;
    }

    /**
     * 获取clazz的fieldName字段
     * @param clazz
     * @param fieldName
     * @param recursive 是否需要向父类查询
     * @return
     */
    public static Field getField(Class clazz, String fieldName, boolean recursive) {
        Field field = null;
        Class self = clazz;

        while (self != null) {
            try {
                field = self.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                logger.debug(fieldName, e);
            }

            if (recursive && field == null) {
                self = clazz.getSuperclass();
            } else {
                break;
            }
        }
        return field;

    }

    /**
     * 获取fieldName字段的值
     * @param target
     * @param fieldName
     * @return
     */
    public static Object getFieldValue(Object target, String fieldName) {
        Field field = getField(target instanceof Class ? (Class) target : target.getClass(), fieldName, true);
        return getFieldValue(target instanceof Class ? null : target, field);
    }

    /**
     * 获取字段的值
     * @param target
     * @param field
     * @return
     */
    public static Object getFieldValue(Object target, Field field) {
        if (field == null) {
            return null;
        }

        field.setAccessible(true);

        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }

        return null;
    }

    /**
     * 调用方法的值
     * @param target
     * @param method
     * @param args
     * @return
     */
    public static Object getMethodValue(Object target, Method method, Object[] args) {
        if (method == null) {
            return null;
        }

        method.setAccessible(true);

        try {
            method.invoke(target, args);
        } catch (IllegalAccessException e) {
            logger.error("", e);
        } catch (InvocationTargetException e) {
            logger.error("", e);
        }

        return null;
    }

    /**
     * 获取clazz的methodName方法
     * @param clazz
     * @param methodName
     * @param args
     * @param recursive 是否需要向父类查询
     * @return
     */
    public static Method getMethod(Class clazz, String methodName, Class[] args, boolean recursive) {
        Method method = null;
        Class self = clazz;

        while (self != null) {

            try {
                method = self.getDeclaredMethod(methodName, args);
            } catch (NoSuchMethodException e) {
                logger.debug(methodName, e);
            }

            if (recursive && method == null) {
                self = clazz.getSuperclass();
            } else {
                break;
            }
        }
        return method;
    }

    /**
     * 获取一个类的字段的偏移值
     * @param clazz
     * @param field
     * @return 返回-1表示无法获取字段的偏移值
     */
    public static long getOffset(Class clazz, Field field) {
        if (getUnsafe() == null || field == null) {
            return -1;
        } else {
            return ((Unsafe) getUnsafe()).objectFieldOffset(field);
        }
    }

    /**
     * 获取一个类的字段的便宜值
     * @param clazz
     * @param fieldName
     * @return 返回-1表示无法获取字段的偏移值
     */
    public static long getOffset(Class clazz, String fieldName) {
        return getOffset(clazz, getField(clazz, fieldName, false));
    }

}
