package com.yuqing.magic.mybatis.provider.base;

import org.apache.ibatis.mapping.MappedStatement;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 通用SQL提供类
 * @author yuqing
 *
 * @since 1.0.1
 */
public class BaseProvider {

    public Class getEntityClass(MappedStatement ms) {
        String msId = ms.getId();
        Class<?> mapperClass = getMapperClass(msId);
        Type[] types = mapperClass.getGenericInterfaces();
        for (Type type : types) {
            if (type instanceof ParameterizedType) {
                ParameterizedType t = (ParameterizedType) type;
                Class<?> returnType = (Class<?>) t.getActualTypeArguments()[0];
                return returnType;
            }
        }
        return null;
    }

    /**
     * 根据msId获取接口类
     *
     * @param msId
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> getMapperClass(String msId) {
        if (msId.indexOf(".") == -1) {
            throw new RuntimeException("当前MappedStatement的id=" + msId + ",不符合MappedStatement的规则!");
        }
        String mapperClassStr = msId.substring(0, msId.lastIndexOf("."));
        try {
            return Class.forName(mapperClassStr);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
