package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.provider.DirtyUpdateProvider;
import org.apache.ibatis.annotations.UpdateProvider;

/**
 * 脏值更新的mapper
 * @author yuqing
 *
 * @since 1.0.1
 */
public interface DirtyUpdateMapper<T> {

    @UpdateProvider(type = DirtyUpdateProvider.class, method = "updateByPrimaryKeyDirtySelectiveSql")
    int updateByPrimaryKeyDirtySelective(T t);
}
