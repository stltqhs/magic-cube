# magic-mybatis
为mybatis提供更丰富的特性

### 配置
在mybatis文件中添加拦截器
```
<plugin interceptor="com.yuqing.magic.mybatis.interceptor.MapperInterceptor">
    <!-- 当实体被@EnableAlternative标记时才支持 -->
    <property name="resultProxy" value="annotation" />

    <!-- 当返回的结果集数量为1时才支持 -->
    <property name="resultSizeProxy" value="1" />
</plugin>
```
### 脏值更新
实体类
```
@Table("table")
@EnableAlternative // 支持脏值更新
public class Person {
    // fields
    // getter/setter
    private String name;
    private Integer age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
}
```
查询之后只更新name字段
```
Person person = sqlSession.selectOne(name, parameter);
person.setName("new name");
sqlSession.update("updateByPrimaryKeyAlternative", person); // 只更新name字段
```

### 版本更新
实体类
```
@Table("table")
public class Person {
    // fields
    // getter/setter
    private String name;
    private Integer age;
    private String gender;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
    public String getGender() {
        return gender;
    }
    public void setGender(String gender) {
        this.gender = gender;
    }
}
```
使用方法
Person person = sqlSession.selectOne(name, parameter);
person.setAge(30); // 假设查询出来时age值为20
// 下面的更新方式的where条件是 where id = #{id} and age = #{oldAge}
sqlSession.update("updateByPrimaryVersionSelective", person);
```
也可以忽略某些字段的版本
```
Person person = sqlSession.selectOne(name, parameter);
person.setAge(30); // 假设查询出来时age值为20
person.setName("new name");
person.setGender("male");
// 下面的更新方式的where条件是 where id = #{id} and age = #{oldAge}
sqlSession.update("updateByPrimaryVersionAlternative", person, "-name,-gender");
```
<code>updateByPrimaryVersionAlternative(Object entity, String version)</code>中的<code>version</code>可以指定版本字段。
“-”表示忽略一个版本字段，“+”表示增加一个版本字段。
如果即没有“-”也没有“+”时表示只考虑该版本字段。如下：
```
"-name,+age" 表示忽略name版本，增加age版本
"gender,age" 表示只考虑gender和age的版本
"" 表示使用变更字段作为版本字段
```