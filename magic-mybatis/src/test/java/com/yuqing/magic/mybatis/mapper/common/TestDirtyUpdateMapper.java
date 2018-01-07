package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.mybatis.entity.Person;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import com.yuqing.magic.persistence.util.PersistenceUtil;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.InputStream;
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
    public void test() throws IOException, SQLException {
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

        int rows = sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonDirtyUpdateMapper.updateByPrimaryKeyDirtySelective",
                person);
    }

}
