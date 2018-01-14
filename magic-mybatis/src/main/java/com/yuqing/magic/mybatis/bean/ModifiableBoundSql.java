package com.yuqing.magic.mybatis.bean;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;

import java.util.List;

/**
 * @author yuqing
 * @date 2018-01-14
 *
 * @since 1.0.1
 */
public class ModifiableBoundSql extends BoundSql {

    protected String sql;

    public ModifiableBoundSql(Configuration configuration, String sql, List<ParameterMapping> parameterMappings, Object parameterObject) {
        super(configuration, sql, parameterMappings, parameterObject);
        this.sql = sql;
    }

    @Override
    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
