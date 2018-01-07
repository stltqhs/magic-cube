# magic-mybatis
为mybatis提供更丰富的特性

### 配置
在mybatis文件中添加拦截器
```
<plugin interceptor="com.yuqing.magic.mybatis.interceptor.MapperInterceptor"></plugin>
```
### 脏值更新
实体类
```
@Table("table")
@ProxyChangeHistory
public class Person {
// fields
// getter/setter
private String name;

public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}
}
```
查询之后只更新name字段
```
Person person = sqlSession.selectOne(name, parameter);