package com.yuqing.magic.mybatis.mapper.common;

/**
 * 具备版本号更新的mapper，以实现乐观锁
 * @author yuqing
 *
 * @since 1.0.1
 */
public interface VersionUpdateMapper {

    int updateByPrimaryKeyVersion(Object object);

}
