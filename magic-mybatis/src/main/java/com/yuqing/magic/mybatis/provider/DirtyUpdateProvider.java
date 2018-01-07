package com.yuqing.magic.mybatis.provider;

import com.yuqing.magic.common.util.CommonUtil;
import com.yuqing.magic.common.util.ReflectionUtil;
import com.yuqing.magic.mybatis.provider.base.BaseProvider;
import com.yuqing.magic.mybatis.proxy.EntityChangeHistoryProxy;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.scripting.xmltags.*;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 脏值更新sql提供类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
public class DirtyUpdateProvider extends BaseProvider {

    private static class MyMixedSqlNode extends MixedSqlNode {

        private Map<String, Object> params = null;

        public MyMixedSqlNode(List<SqlNode> contents) {
            super(contents);
        }

        public MyMixedSqlNode(List<SqlNode> contents, Map<String, Object> params) {
            super(contents);

            this.params = params;
        }

        @Override
        public boolean apply(DynamicContext context) {
            if (params != null) {
                Iterator<Map.Entry<String, Object>> iterator = params.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, Object> item = iterator.next();
                    context.bind(item.getKey(), item.getValue());
                }
            }
            return super.apply(context);
        }
    }

    public String updateByPrimaryKeyDirtySelectiveSql(Object record) {
        return "updateByPrimaryKeyDirtySelective";
    }

    public SqlNode updateByPrimaryKeyDirtySelective(MappedStatement ms, Object args[]) {
        Class entityClass = getEntityClass(ms);

        List<SqlNode> sqlNodeList = new LinkedList<>();

        Map<String, Object> params = new HashMap<>();

        sqlNodeList.add(new StaticTextSqlNode("UPDATE " + PersistenceUtil.getTableName(entityClass)));

        EntityChangeHistoryProxy historyProxy = EntityChangeHistoryProxy.extract(args[0]);

        if (historyProxy == null) {
            throw new IllegalArgumentException(entityClass.getCanonicalName() + " is not an " + EntityChangeHistoryProxy.class.getName());
        }

        appendSetSqlNode(sqlNodeList, entityClass, historyProxy, ms, params);

        appendWhereSqlNode(sqlNodeList, entityClass, args[0], ms, params);

        return new MyMixedSqlNode(sqlNodeList, params);
    }

    private void appendWhereSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                    Object record,
                                    MappedStatement ms,
                                    Map<String, Object> params) {

        List<SqlNode> whereNodes = new LinkedList<>();
        List<Field> fields = PersistenceUtil.getIdFields(clazz);

        if (!CommonUtil.isNullOrEmpty(fields)) {
            String prefix = "where_" + PersistenceUtil.getTableName(clazz)
                    + (int)(Math.random() * 1000);
            String p = "";
            for (Field field : fields) {
                String column = PersistenceUtil.getColumnName(clazz, field);
                String variable = prefix + column;
                whereNodes.add(new StaticTextSqlNode(p + column + " = #{" + variable + "}"));
                params.put(variable, ReflectionUtil.getFieldValue(record, field));
                p = " AND ";
            }
        }

        sqlNodes.add(new WhereSqlNode(ms.getConfiguration(), new MixedSqlNode(whereNodes)));
    }

    private void appendSetSqlNode(List<SqlNode> sqlNodes, Class clazz,
                                  EntityChangeHistoryProxy historyProxy,
                                  MappedStatement ms,
                                  Map<String, Object> params) {
        Iterator<Map.Entry<String, List<?>>> iterator =
                historyProxy.getChangeHistory().entrySet().iterator();
        String prefix = "set_" + PersistenceUtil.getTableName(clazz)
                + (int)(Math.random() * 1000);

        List<SqlNode> ifNodes = new LinkedList<>();
        while (iterator.hasNext()) {
            Map.Entry<String, List<?>> item = iterator.next();
            String variable = prefix + item.getKey();
            ifNodes.add(new StaticTextSqlNode(PersistenceUtil.getColumnName(clazz, item.getKey())
                    + " = #{" + variable + "},"));

            params.put(variable, CommonUtil.getLast(item.getValue()));
        }

        sqlNodes.add(new SetSqlNode(ms.getConfiguration(), new MixedSqlNode(ifNodes)));
    }

}
