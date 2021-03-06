package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.entity.Person;
import tk.mybatis.mapper.common.Mapper;

/**
 * @author yuqing
 *
 * @since 1.0.1
 */
public interface PersonAlternativeUpdateMapper
        extends AlternativeUpdateMapper<Person>,
        Mapper<Person>,
        VersionUpdateMapper<Person>,
        InsertOnDuplicateKeyUpdateMapper<Person> {
}
