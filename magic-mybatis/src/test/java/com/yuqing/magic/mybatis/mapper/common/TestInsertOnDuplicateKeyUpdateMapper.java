package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.entity.Person;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author yuqing
 * @date 2018-01-13
 *
 * @since 1.0.1
 */
@RunWith(JUnit4.class)
public class TestInsertOnDuplicateKeyUpdateMapper {

    @Test
    public void test() throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, SQLException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory =
                new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession sqlSession = sqlSessionFactory.openSession();

        sqlSession.getConnection().createStatement().execute(
                "create table if not exists t_person(" +
                        "id bigint primary key," +
                        "name varchar(20)," +
                        "gender varchar(20)," +
                        "birthday datetime," +
                        "money decimal(10,2))");

        sqlSession.getConnection().createStatement().execute(
                "delete from t_person where id = 1");

        Person p1 = new Person();
        p1.setId(1L);
        p1.setName("Chun");
        p1.setBirthday(new Date());
        p1.setGender("男");
        p1.setMoney(new BigDecimal(1.2));

        MybatisUtil.justReturn(1);

        int rows = sqlSession.insert("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.insertOnDuplicateKeyUpdate", p1);

        MappedStatement ms = sqlSession.getConfiguration().getMappedStatement("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.insertOnDuplicateKeyUpdate");

        Assert.assertEquals(1, rows);

        BoundSql boundSql = ms.getBoundSql(p1);

        String sql = boundSql.getSql();

        Assert.assertEquals("INSERT INTO t_person ( id,name,gender,birthday,money ) VALUES( ?,?,?,?,? )  ON DUPLICATE KEY UPDATE    name = ?,gender = ?,birthday = ?,money = ?",
                sql);

        Person p2 = new Person();
        p2.setName("yuqing");
        p2.setId(1L);

        MybatisUtil.justReturn(1);

        rows = sqlSession.insert("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.insertSelectiveOnDuplicateKeyUpdate", p2);

        ms = sqlSession.getConfiguration().getMappedStatement("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.insertSelectiveOnDuplicateKeyUpdate");

        boundSql = ms.getBoundSql(p2);

        sql = boundSql.getSql();

        Assert.assertEquals("INSERT INTO t_person ( id,name ) VALUES( ?,? )  ON DUPLICATE KEY UPDATE    name = ?",
                sql);
    }

}
