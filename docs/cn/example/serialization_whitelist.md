序列化白名单
==

增对目前序列化漏洞频发的情况，默认启用了反序列化白名单

>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

## 1 序列化白名单组成

### 1.1 白名单文件

#### 1.1.1 系统内置的白名单文件

定义在"META-INF/permission/serialization.whitelist"中

包括java的常用类型和joyrpc自带的数据类型

```text
#### java
##
int
byte
short
long
float
double
boolean
char
void
java.lang.Integer
java.lang.Byte
java.lang.Short
......
```

#### 1.1.2 用户定义的白名单文件

定义在classpath的"permission/serialization.whitelist"中

### 1.2 自动扫描接口类

获取方法参数、返回值涉及的类型，如果该类型是复杂的对象类型，则会递归扫描其能序列化的字段类型。

会根据泛型类型来自动识别泛型变量

### 1.3 枚举类型

枚举类型默认进入白名单

### 1.4 异常

异常类型默认进入白名单

## 2 序列化白名单开关

默认启用了序列化白名单，可以通过如下开关来进行关闭

在环境变量、JVM参数、全局参数或Springboot应用中的配置项设置参数如下

```properties
serializer.whitelist.enabled=false
```
