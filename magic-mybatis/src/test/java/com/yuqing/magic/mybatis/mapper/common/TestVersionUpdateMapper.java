package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.entity.Person;
import com.yuqing.magic.mybatis.util.MybatisUtil;
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
 * @author YutsingLee
 * @date 2018-01-13
 *
 * @since 1.0.1
 */
@RunWith(JUnit4.class)
public class TestVersionUpdateMapper {
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

        Person p1 = new Person();
        p1.setId(1L);
        p1.setName("Chun");
        p1.setBirthday(new Date());
        p1.setGender("男");
        p1.setMoney(new BigDecimal(1.2));

        sqlSession.insert("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.insertSelective", p1);

        Person person2 = sqlSession.selectOne("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.selectByPrimaryKey", 1L);

        Assert.assertNotNull(person2);

        person2.setName("yuqing");
        person2.setMoney(new BigDecimal(2.5));

        VersionUpdateMapper mapper = sqlSession.getMapper(PersonAlternativeUpdateMapper.class);

        int rows = mapper.updateByPrimaryKeyVersionSelective(person2, "-name");

        Assert.assertEquals(1, rows);
    }

}
