package com.yuqing.magic.common.util;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * 通用工具类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class CommonUtil {

    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    /**
     * 提取List元素的某个字段
     * @param collection
     * @param fieldName
     * @param keyType
     * @param <T>
     * @return
     */
    public static <T> List<T> extractFields(List<? extends Object> collection, String fieldName, Class<T> keyType) {
        if (collection == null || collection.size() == 0) return null;
        List<T> retList = new ArrayList(collection.size());

        for (Object item : collection) {
            Object value = null;
            if (item instanceof Map) {
                if (((Map)item).containsKey(fieldName))
                    value = ((Map) item).get(fieldName);
            } else {
                value = ReflectionUtil.getFieldValue(item, fieldName);
            }

            if (value == null) {
                continue;
            }

            if (keyType == null || value.getClass().isAssignableFrom(keyType)) {
                retList.add((T) value);
                continue;
            }
            if (keyType == String.class) {
                retList.add((T) value.toString());
                continue;
            }
            if (Number.class.isAssignableFrom(keyType)) {
                String val = value.toString();
                if (StringUtils.isNumeric(val.replace(".", ""))) {
                    Constructor<?> constructor = null;
                    try {
                        constructor = keyType.getConstructor(String.class);
                    } catch (NoSuchMethodException e) {
                        throw new IllegalArgumentException("Can not get Constructor from " + keyType.getCanonicalName() + " with String.class");
                    }
                    if (constructor != null) {
                        try {
                            retList.add((T) constructor.newInstance(val));
                        } catch (Exception e) {
                            logger.error("", e);
                        }
                    }
                }
                continue;
            }
            throw new ClassCastException("Can not cast " + value.getClass().getCanonicalName() + " to " + keyType.getCanonicalName());
        }
        return retList;
    }

    /**
     * 提取集合元素的某个字段
     * @param collection
     * @param fieldName
     * @param <T>
     * @return
     */
    public static <T> List<T> extractFields(List<? extends Object> collection, String fieldName) {
        return extractFields(collection, fieldName, null);
    }

    /**
     * 将集合一某个字段转换为Map类型
     * @param collection
     * @param fieldName
     * @param <K>
     * @param <O>
     * @return
     */
    public static <K, O> Map<K, O> fieldMap(List<O> collection, String fieldName) {
        if (collection == null || collection.size() == 0) return null;
        Map<K, O> retMaps = new HashMap();
        for (O item : collection) {
            if (item instanceof Map) {
                if (((Map)item).containsKey(fieldName))
                    retMaps.put((K)((Map)item).get(fieldName), item);
            } else {
                K fieldValue = (K) ReflectionUtil.getFieldValue(item, fieldName);
                if (fieldValue != null)
                    retMaps.put(fieldValue, item);
            }
        }
        return retMaps;
    }

    /**
     * URL拼接
     * @param parts
     * @return
     */
    public static String concatUrl(String ...parts) {
        if (parts == null || parts.length == 0) return null;
        StringBuilder buffer = new StringBuilder();

        String pre = null;
        for (String p : parts) {
            if (pre != null) {
                if (pre.endsWith("/") && p.startsWith("/")) {
                    // 去掉p的首部斜线
                    p = p.substring(1);
                } else if (!pre.endsWith("/") && !p.startsWith("/")) {
                    buffer.append("/");
                }
            }
            buffer.append(p);
        }

        return buffer.toString();
    }

    /**
     * 判断t是否为null或者空的数组，集合，映射
     * @param t
     * @param <T>
     * @return
     */
    public static <T> boolean isNullOrEmpty(T t) {
        if (t == null) {
            return true;
        }
        if (t.getClass().isArray() && isEmptyArray(t)) {
            return true;
        }
        if (t instanceof Collection && isEmptyCollection((Collection)t)) {
            return true;
        }
        if (t instanceof Map && isEmptyMap((Map)t)) {
            return true;
        }
        return false;
    }

    public static boolean isEmptyCollection(Collection collection) {
        return collection.size() == 0;
    }

    public static boolean isEmptyMap(Map map) {
        return map.size() == 0;
    }

    public static <T> boolean isEmptyArray(T t) {
        if (t == null || !t.getClass().isArray()) {
            throw new IllegalArgumentException("argument is null or not array type.");
        }

        return Array.getLength(t) == 0;
    }

    /**
     * 获取对象input的大小，如果input为数组，返回数组长度，如果为集合或者Map，返回size()方法的值。
     * 如果为其他对象，返回其成员变量大小
     * @param input
     * @return
     */
    public static int lengthOf(Object input) {
        if (input == null) {
            return 0;
        }
        if (input.getClass().isArray()) {
            return Array.getLength(input);
        }
        if (input instanceof Collection) {
            return ((Collection) input).size();
        }
        if (input instanceof Map) {
            return ((Map) input).size();
        }
        return memberLength(input);
    }

    private static int memberLength(Object input) {
        if (input == null) {
            return 0;
        }

        return 1;
    }


    public static <T> List<T> toList(T[] array) {
        if (array == null) {
            return null;
        }
        List<T> list = new ArrayList<>(array.length);

        for (T t : array) {
            list.add(t);
        }

        return list;
    }

    public static <T> List<T> toList(Set<T> sets) {
        if (sets == null) {
            return null;
        }
        List<T> list = new ArrayList<>(sets.size());

        for (T t : sets) {
            list.add(t);
        }

        return list;
    }

    public static <T> T[] toArray(Collection<T> list, Class<T> type) {
        if (list == null) {
            return null;
        }
        T[] array = (T[]) Array.newInstance(type, list.size());

        list.toArray(array);

        return array;
    }

    public static <T> T getLast(Collection<T> list) {
        int length = lengthOf(list);

        if (length == 0) {
            return null;
        }

        if (list instanceof List) {
            return (T) ((List) list).get(length - 1);
        }

        if (list instanceof Set) {
            return (T) ((List) list).get(length - 1);
        }

        Object arr[] = list.toArray();

        return (T) arr[length - 1];
    }

    public static <T> T getFirst(Collection<T> list) {
        int length = lengthOf(list);

        if (length == 0) {
            return null;
        }

        if (list instanceof List) {
            return (T) ((List) list).get(0);
        }

        if (list instanceof Set) {
            return (T) ((List) list).get(0);
        }

        Object arr[] = list.toArray();

        return (T) arr[0];
    }
}
