# magic-common
Java通用类，包括工具类等

### HTTP客户端工具
```
String body = HttpClientUtil.get(url); // 同步请求

Future<String> futureBody = HttpClientUtil.getAsync(url); // 异步请求
String fbody = futureBody.get(); // 等待数据返回
```

### 日期工具
```
// 格式化秒数
DateTimeUtil.second2Human(183, "%h小时%m分%s秒", DateTimeUtil.DEFAULT_STYLE); // 返回 "0小时3分3秒"
```

### 反射工具
```
// 获取Unsafe
ReflectionUtil.getUnsafe(); // 返回的是Object类型

// 获取字段的值
Object value = ReflectionUtil.getFieldValue(target, "fieldName");

// 获取字段的偏移值
long offset = ReflectionUtil.getOffset(clazz, "fieldName");
```

### 数据摘要工具
```
// 获取字符串的md5值
String md5 = MD5Util.encode("string");
```

### 数值工具
```
// 判断字符串是否为数字
boolean isNumber = NumberUtil.isNumber("123"); // 返回 true
// atoi
Integer number = NumberUtil.safeParseInteger("123"); // 返回 123
```