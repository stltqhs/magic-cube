package com.yuqing.magic.mybatis.util;

import com.yuqing.magic.mybatis.entity.Person;
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
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;

/**
 * @author yuqing
 * @date 2018-01-13
 *
 * @since 1.0.1
 */
@RunWith(JUnit4.class)
public class MybatisUtilTest {

    @Test
    public void sqlModifier() throws IOException, SQLException {
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

        int rows = sqlSession.insert("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.insertSelective", p1);

        Assert.assertEquals(1, rows);

        Person p2 = new Person();
        p2.setId(1L);
        p2.setMoney(new BigDecimal(3));

        MybatisUtil.change("money", "money = money +");
        rows = sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.updateByPrimaryKeySelective", p2);

        Assert.assertEquals(1, rows);

        Person p3 = sqlSession.selectOne("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.selectByPrimaryKey", 1L);

        Assert.assertNotNull(p3);

        Assert.assertEquals(4.2, p3.getMoney().doubleValue(), 0.001);

        sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.updateByPrimaryKeySelective", p3);

        boolean throwException = false;
        try {
            MybatisUtil.lockForUpdate();
            sqlSession.selectOne("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.selectByPrimaryKey", 1L);
        } catch (Exception e) {
            throwException = true;
            Assert.assertTrue(e.getMessage().indexOf("FOR UPDATE") != -1);
        }

        Assert.assertTrue(throwException);

        sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.updateByPrimaryKeySelective", p3);

        Person p4 = sqlSession.selectOne("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.selectByPrimaryKey", 1L);

        Assert.assertNotNull(p4);
    }

}
