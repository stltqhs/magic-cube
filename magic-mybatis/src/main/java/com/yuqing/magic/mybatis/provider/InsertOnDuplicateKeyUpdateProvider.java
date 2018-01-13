package com.yuqing.magic.mybatis.provider;

import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.xmltags.MixedSqlNode;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.scripting.xmltags.StaticTextSqlNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 *
 *
 * @author yuqing
 * @date 2018-01-13
 *
 * @since 1.0.1
 */
public class InsertOnDuplicateKeyUpdateProvider extends BaseProvider {

    private static final Logger logger = LoggerFactory.getLogger(InsertOnDuplicateKeyUpdateProvider.class);

    private static final StaticTextSqlNode NO_DUPLICATE_KEY_UPDATE_NODE = new StaticTextSqlNode(" ON DUPLICATE KEY UPDATE ");

    public String insertOnDuplicateKeyUpdateSql() {
        return "insertOnDuplicateKeyUpdate";
    }

    public String insertSelectiveOnDuplicateKeyUpdateSql() {
        return "insertSelectiveOnDuplicateKeyUpdate";
    }

    public SqlNode insertOnDuplicateKeyUpdate(MappedStatement ms) {
        Class entityClass = getEntityClass(ms);

        if (logger.isDebugEnabled()) {
            logger.debug("create SqlNode for {}", ms.getId());
        }

        List<SqlNode> sqlNodeList = new LinkedList<>();

        // insert into
        sqlNodeList.add(new StaticTextSqlNode("INSERT INTO " + PersistenceUtil.getTableName(entityClass)));

        // (col1,col2,...)
        MybatisUtil.appendInsertColumnSqlNode(sqlNodeList, entityClass, ms, false);

        // values(#{col1},#{col2},...)
        MybatisUtil.appendValuesSqlNode(sqlNodeList, entityClass, ms, null, false);

        // on duplicate key update
        sqlNodeList.add(NO_DUPLICATE_KEY_UPDATE_NODE);
        MybatisUtil.appendSetSqlNode(sqlNodeList, entityClass, ms, null, false, true);

        return new MixedSqlNode(sqlNodeList);
    }

    public SqlNode insertSelectiveOnDuplicateKeyUpdate(MappedStatement ms) {
        Class entityClass = getEntityClass(ms);

        if (logger.isDebugEnabled()) {
            logger.debug("create SqlNode for {}", ms.getId());
        }

        List<SqlNode> sqlNodeList = new LinkedList<>();

        // insert into table
        sqlNodeList.add(new StaticTextSqlNode("INSERT INTO " + PersistenceUtil.getTableName(entityClass)));

        // (col1,col2,...)
        MybatisUtil.appendInsertColumnSqlNode(sqlNodeList, entityClass, ms, true);

        // values(#{col1},#{col2},...)
        MybatisUtil.appendValuesSqlNode(sqlNodeList, entityClass, ms, null, true);

        // on duplicate key update
        sqlNodeList.add(NO_DUPLICATE_KEY_UPDATE_NODE);
        MybatisUtil.appendSelectiveSetSqlNode(sqlNodeList, entityClass, ms, null, true);


        return new MixedSqlNode(sqlNodeList);
    }
}
