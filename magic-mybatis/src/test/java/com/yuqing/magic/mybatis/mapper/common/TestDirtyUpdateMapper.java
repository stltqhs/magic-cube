package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.entity.Person;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import org.apache.ibatis.io.Resources;
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
 * DirtyUpdateMapper测试类
 *
 * @author yuqing
 *
 * @since 1.0.1
 */
@RunWith(JUnit4.class)
public class TestDirtyUpdateMapper {

    @Test
    public void test() throws IOException, SQLException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory =
                new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession sqlSession = sqlSessionFactory.openSession();

        Person person = MybatisUtil.proxyEntityChangeHistory(new Person());

        person.setId(123L);
        person.setBirthday(new Date());
        person.setName("yuqing");

        sqlSession.getConnection().createStatement().execute(
                "create table if not exists t_person(" +
                        "id bigint primary key," +
                        "name varchar(20)," +
                        "gender varchar(20)," +
                        "birthday datetime," +
                        "money decimal(10,2))");

        sqlSession.getConnection().createStatement().execute(
                "delete from t_person where id = 1");

        int rows = 0;

        rows = sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonDirtyUpdateMapper.updateByPrimaryKeyDirtySelective",
                person);

        Assert.assertEquals(0, rows);

        Person p1 = new Person();
        p1.setId(1L);
        p1.setName("Chun");
        p1.setBirthday(new Date());
        p1.setGender("男");
        p1.setMoney(new BigDecimal(1.2));

        rows = sqlSession.insert("com.yuqing.magic.mybatis.mapper.common.PersonDirtyUpdateMapper.insertSelective", p1);

        Assert.assertEquals(1, rows);

        Person person2 = sqlSession.selectOne("com.yuqing.magic.mybatis.mapper.common.PersonDirtyUpdateMapper.selectByPrimaryKey", 1L);

        Assert.assertNotNull(person2);

        person2.setName("yuqing");

        rows = sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonDirtyUpdateMapper.updateByPrimaryKeyDirtySelective",
                person2);

        Assert.assertEquals(1, rows);
    }

}