package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.provider.InsertOnDuplicateKeyUpdateProvider;
import org.apache.ibatis.annotations.InsertProvider;

/**
 * @author yuqing
 *
 * @date 2018-01-13
 *
 * @since 1.0.1
 */
public interface InsertOnDuplicateKeyUpdateMapper<T> {

    @InsertProvider(type= InsertOnDuplicateKeyUpdateProvider.class, method= "insertOnDuplicateKeyUpdateSql")
    int insertOnDuplicateKeyUpdate( T t);

    @InsertProvider(type = InsertOnDuplicateKeyUpdateProvider.class, method = "insertSelectiveOnDuplicateKeyUpdateSql")
    int insertSelectiveOnDuplicateKeyUpdate( T t);

}
