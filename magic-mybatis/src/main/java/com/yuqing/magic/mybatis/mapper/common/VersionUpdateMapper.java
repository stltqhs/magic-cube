package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.provider.VersionUpdateProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.UpdateProvider;

/**
 * 具备版本号更新的mapper，以实现乐观锁
 * @author yuqing
 *
 * @since 1.0.1
 */
public interface VersionUpdateMapper<T> {

    @UpdateProvider(type = VersionUpdateProvider.class, method = "updateByPrimaryKeyVersionSelectiveSql")
    int updateByPrimaryKeyVersionSelective(@Param("entity") T t, @Param("version") String version);

    @UpdateProvider(type = VersionUpdateProvider.class, method = "updateByPrimaryKeyVersionAlternativeSql")
    int updateByPrimaryKeyVersionAlternative(@Param("entity") T t, @Param("version") String version);

}
