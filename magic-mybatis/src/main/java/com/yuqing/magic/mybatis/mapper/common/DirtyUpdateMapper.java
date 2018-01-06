package com.yuqing.magic.mybatis.mapper.common;

/**
 * 脏值更新的mapper
 * @author yuqing
 *
 * @since 1.0.1
 */
public interface DirtyUpdateMapper<T> {

    int updateByPrimaryKeyDirtySelective(T t);
}
