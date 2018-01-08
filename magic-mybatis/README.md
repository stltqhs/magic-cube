# magic-mybatis
为mybatis提供更丰富的特性

### 配置
在mybatis文件中添加拦截器
```
<plugin interceptor="com.yuqing.magic.mybatis.interceptor.MapperInterceptor">
    <!-- 当实体被@ProxyChangeHistory标记时才支持 -->
    <property name="resultProxy" value="annotation" />

    <!-- 当返回的结果集数量为1时才支持 -->
    <property name="resultSizeProxy" value="1" />
</plugin>
```
### 脏值更新
实体类
```
@Table("table")
@ProxyChangeHistory // 支持脏值更新
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
sqlSession.update("updateByPrimaryKeyDirtySelective", person); // 只更新name字段
```