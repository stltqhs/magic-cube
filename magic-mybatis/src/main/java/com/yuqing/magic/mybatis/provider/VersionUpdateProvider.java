package com.yuqing.magic.mybatis.provider;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;

import java.util.LinkedList;
import java.util.List;

/**
 * 版本更新的提供类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class VersionUpdateProvider {

    public String updateByPrimaryKeyVersionSql(Object record) {
        return "updateByPrimaryKeyVersion";
    }

    public SqlNode updateByPrimaryKeyVersion(MappedStatement ms) {
        List<SqlNode> sqlNodes = new LinkedList<>();

        return new MixedSqlNode(sqlNodes);
    }

}
