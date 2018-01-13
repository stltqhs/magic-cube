package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.provider.AlternativeUpdateProvider;
import org.apache.ibatis.annotations.UpdateProvider;

/**
 * 脏值更新的mapper
 * @author yuqing
 *
 * @since 1.0.1
 */
public interface AlternativeUpdateMapper<T> {

    @UpdateProvider(type = AlternativeUpdateProvider.class, method = "updateByPrimaryKeyAlternativeSql")
    int updateByPrimaryKeyAlternative(T t);
}
