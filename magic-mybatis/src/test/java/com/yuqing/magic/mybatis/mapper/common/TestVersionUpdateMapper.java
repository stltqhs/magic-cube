package com.yuqing.magic.mybatis.mapper.common;

import com.yuqing.magic.common.util.DateTimeUtil;
import com.yuqing.magic.mybatis.entity.Person;
import com.yuqing.magic.mybatis.util.MybatisUtil;
import org.apache.ibatis.annotations.Param;
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
import java.util.Calendar;
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
        p1.setBirthday(DateTimeUtil.getDate(Calendar.YEAR, -29));
        p1.setGender("男");
        p1.setMoney(new BigDecimal(1.2));

        sqlSession.insert("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.insertSelective", p1);

        Person person2 = sqlSession.selectOne("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.selectByPrimaryKey", 1L);

        Assert.assertNotNull(person2);

        person2.setName("yuqing");
        person2.setMoney(new BigDecimal(2.5));

        Person fp1 = new Person();
        fp1.setId(1L);
        fp1.setName("yuqing2");
        sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.updateByPrimaryKeySelective", fp1);

        VersionUpdateMapper mapper = sqlSession.getMapper(PersonAlternativeUpdateMapper.class);

        // 虽然name字段被更新了，但是这里忽略name字段的版本，所以依然更新成功
        int rows = mapper.updateByPrimaryKeyVersionSelective(person2, "-name");

        Assert.assertEquals(1, rows);

        Person person3 = sqlSession.selectOne("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.selectByPrimaryKey", 1L);

        person3.setName("Floyd");
        person3.setMoney(new BigDecimal(3.7));

        Person fp2 = new Person();
        fp2.setId(1L);
        fp2.setBirthday(new Date());
        sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.updateByPrimaryKeySelective", fp1);

        // 由于birthday字段更新了，进行person3并没有修改该字段，但是考虑了该字段的版本，所以更新将失败
        rows = mapper.updateByPrimaryKeyVersionSelective(person3, "+birthday");

        Assert.assertEquals(0, rows);

        Person fp3 = new Person();
        fp3.setBirthday(DateTimeUtil.getDate(Calendar.YEAR, -10));
        fp3.setId(1L);

        Person person4 = sqlSession.selectOne("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.selectByPrimaryKey", 1L);

        person4.setName("Floyd");
        person4.setBirthday(DateTimeUtil.getDate(Calendar.HOUR, -20));

        sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.updateByPrimaryKeySelective", fp3);

        // person4修改了3个字段，分别是name、money和birthday，但是birthday被fp3修改

        rows = mapper.updateByPrimaryKeyVersionSelective(person4, "name,money"); // 由于忽略了birthday，索引依然可以更新成功

        Assert.assertEquals(1, rows);

        sqlSession.update("com.yuqing.magic.mybatis.mapper.common.PersonAlternativeUpdateMapper.updateByPrimaryKeyVersionSelective",
                new Object[]{person4, "name,money"});
    }

}
