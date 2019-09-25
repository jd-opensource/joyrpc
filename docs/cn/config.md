配置参考手册
===

- 引入joyrpc的schema，参考如下
````
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:joyrpc="http://joyrpc.io/schema/joyrpc"
       xsi:schemaLocation=
       "http://www.springframework.org/schema/beans  http://www.springframework.org/schema/beans/spring-beans.xsd
       http://joyrpc.io/schema/joyrpc  
       http://joyrpc.io/schema/joyrpc/joyrpc.xsd">
</beans>
````
#### 注册中心配置 
标签：`<joyrpc:registry>`

|标签|属性|类型|必填|默认值|描述|
| -- | --| -- | -- | -- | -- |
|registry|id|string|**是**||Spring的BeanId|
|registry|address|string|**是**||注册中心地址|
|registry|registry|string|**是**|memory|注册中心类型|

>1.一级元素，下面可以有parameter节点。对应io.joyrpc.config.RegistryConfig
2.配置注册中心用，注册服务和订阅服务，订阅配置。 全局唯一即可。

```xml
<!-- 注册中心 -->
<joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
```

#### 服务server配置  
标签：`<joyrpc:server>` 

|标签|属性|类型|必填|默认值|描述|
| -- | --| -- | -- | -- | -- |
|server|id|string|**是**||Spring的BeanId|
|server|host|string|否||服务端绑定地址|
|server|port|int|否|22000|服务端绑定端口。如果端口被占用，会提示启动失败|
|server|contextPath|string|否||发布上下文。用于基于http的协议。|
|server|coreThreads|int|否|20|业务线程池core线程数|
|server|maxThreads|int|否|int|业务线程池最大线程数|
|server|threadPool|string|否|adaptive|线程池插件名称|
|server|ioThreads|int|否|0|IO线程池大小，程序中默认max(8,cpu+1)|
|server|queues|int|否|0|业务线程池队列大小。0表示无队列，正整数表示有限队列|
|server|accepts|int|否|2147483647|允许的TCP长连接数（包括http），不能填写小于0的值|
|server|buffers|int|否|8192|IO的缓冲区大小，最大：32768，最小：1024|
|server|epoll|int|否|false|Linux下是否启动epoll特性|
|server|queueType|string|否|normal|业务线程池队列类型|

>1.一级元素，下面可以有parameter节点。对应io.joyrpc.config.ServerConfig
2.配置服务端用，只在发布服务端时候声明。
3.默认为joyrpc协议，不需要再设置。
4.一个server下可以发布多个provider。

```xml
<!-- 注册中心 -->
<joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
<!-- server配置 -->
<joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
```

#### 服务提供方配置
标签：`<joyrpc:provider>` 

|标签|属性|类型|必填|默认值|描述|
| -- | --| -- | -- | :- | :-- |
|provider|id|string|**是**||Spring的BeanId|
|provider|interface|string|**是**||发布的接口名称|
|provider|alias|string|**是**||服务别名分组信息|
|provider|ref|ref|**是**||接口实现类|
|provider|server|ref|**是**|全部server|需要发布服务到的Server，一个server对应一个端口|
|provider|filter|ref|否||自定义过滤器实现插件名称|
|provider|registry|ref|否||注册中心引用，多个用英文逗号隔开|
|provider|register|boolean|否|true|是否注册到注册中心|
|provider|subscribe|boolean|否|true|是否从注册中心订阅|
|provider|timeout|Int|否|5000|服务端调用超时时间，单位毫秒。|
|provider|proxy|string|否|bytebuddy|代理类, 插件名称：bytebuddy、javassist、jdk|
|provider|cache|boolean|否||是否开启结果缓存。如果开启需要指定cacheProvider|
|provider|cacheProvider|string|否|caffeine|缓存插件名称： caffeine、guava|
|provider|cacheKeyGenerator|string|否|default|cache key生成器名称|
|provider|cacheExpireTime|long|否|-1|cache过期时间，单位ms 毫秒|
|provider|cacheNullable|boolean|否|false|结果缓存值是否可空|
|provider|cacheCapacity|int|否|10000|结果缓存容量大小|
|provider|delay|int|否|0|延迟发布服务时间。|
|provider|weight|int|否|100|服务提供者权重|
|provider|include|string|否|*|发布的方法列表，逗号分隔|
|provider|exclude|string|否||不发布的方法列表，逗号分隔|
|provider|concurrency|int|否|-1|接口下每方法的最大可并行执行请求数, -1: 关闭，0: 开启但不限制|
|provider|validation|boolean|否|false|是否校验参数|
|provider|compress|string|否|lz4|压缩算法插件 lz4、snappy、lzma、zlib|
|provider|interfaceValidator|string|否|standard|接口验证器插件名称，同validation参数配合使用|
|provider|warmup|string|否|standard|预热插件名称|

>一级元素，下面可以有method或者parameter节点。对应io.rpc.config.ProviderConfig
>发布joyrpc服务Provider使用。 

```xml
<!-- 注册中心 -->
<joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
<!-- server配置 -->
<joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
<!-- 接口实现配置 -->
<bean id="demoServiceImpl" class="io.joyrpc.service.impl.DemoServiceImpl"/>
<!-- provider配置 -->
<joyrpc:provider id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" ref="demoServiceImpl" server="myJoy"></joyrpc:provider>
```

#### 消费者配置
标签：`<joyrpc:consumer>` 

|标签|属性|类型|必填|默认值|描述|
| -- | --| -- | -- | :- | :-- |
|consumer|id|string|**是**||Spring的BeanId|
|consumer|interface|string|**是**||调用的接口名称|
|consumer|alias|string|**是**||服务别名分组信息|
|consumer|url|string|否||直连地址，配置了此地址就不再从注册中心获取|
|consumer|ilter|ref|否||过滤器插件名称，多个用英文逗号隔开|
|consumer|registry|ref|**是**||注册中心引用 （只能选择一个）|
|consumer|register|boolean|否|true|是否注册到注册中心|
|consumer|subscribe|boolean|否|true|是否从注册中心订阅|
|consumer|timeout|int|否|5000|调用端调用超时时间，单位毫秒|
|consumer|proxy|string|否|bytebuddy|代理类生成方式插件配置，插件名称：bytebuddy、javassist、jdk|
|consumer|cache|boolean|否|false|是否开启结果缓存。如果开启需要指定cacheProvider|
|consumer|cacheProvider|string|否|caffeine|自定义结果缓存插件名称：caffeine、guava|
|consumer|cacheKeyGenerator|string|否|default|cache key生成器名称|
|consumer|cacheExpireTime|long|否|-1|cache过期时间，单位ms 毫秒|
|consumer|cacheNullable|boolean|否|false|结果缓存值是否可空|
|consumer|cacheCapacity|int|否|10000|结果缓存容量大小|
|consumer|generic|boolean|否|false|是否泛化调用|
|consumer|cluster|string|否|failfast|集群策略插件名称，已支持：failover、failfast 方式|
|consumer|retries|int|否|0（0表示失败后不重试）|失败后重试次数（需要和cluster=failover结合使用，单实例设置retries无效）|
|consumer|loadbalance|string|否|randomWeight|负载均衡算法插件名称：roundRobin、randomWeight、adaptive|
|consumer|sticky|boolean|否|false|是否粘滞连接（除非断开连接，只调一个）|
|consumer|injvm|boolean|否|true|是否走injvm调用（如果同一jvm内发布了服务，则不走远程调用）|
|consumer|check|boolean|否|false|是否强依赖服务端（无可用服务端启动失败）|
|consumer|serialization|string|否|hessian|序列化插件名称：protostuff、msgpack、json@fastjson、fst、kryo、hessian、java，选用protostuff 性能更高，使用时需要注意数组、集合中不能有null元素|
|consumer|router|string|否|methodRouter|路由规则插件名称，已支持 methodRouter(基于方法参数的路由)|
|consumer|concurrency|int|否|-1|接口下**每方法**的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制|
|consumer|validation|boolean|否|false|是否校验参数|
|consumer|compress|string|否|lz4|压缩算法插件名称：lz4、snappy、lzma、zlib|
|consumer|interfaceValidator|string|否|standard|接口验证器插件名称，同validation参数配合使用|
|consumer|initSize|int|否|10|初始化连接数|
|consumer|minSize|int|否|0|最小连接数|
|consumer|candidature|string|否|""|候选者算法插件|
|consumer|failoverWhenThrowable|string|否|""|可以重试的逗号分隔的异常全路径类名，多个class用,分割，（需要和cluster=failover结合使用）|
|consumer|warmupWeight|int|否|0|预热权重，默认取consumer端用户设置；用户未设置时，取provider端设置的权重|
|consumer|warmupDuration|int|否|60000|单位时间内预热权重到达100，默认60000毫秒（即1分钟）|
|consumer|failoverPredication|string|否||重试异常判断接口插件|

>作为一级元素，下面可以有method或者parameter节点。对应io.joyrpc.config.ConsumerConfig
>作为二级元素，ConsumerGroup下的元素。
>发布joyrpc服务Consumer使用。

```xml
<!-- 注册中心 -->
<joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
<!-- server配置 -->
<joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
<!-- consumer配置 -->
<joyrpc:consumer id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" registry="joyRpcRegistry"></joyrpc:consumer>
```

#### 方法配置
标签：`<joyrpc:method>` 

|标签|属性|类型|必填|默认值|父标签|描述|
| -- | --| -- | -- | :- | :-- | --- |
|method|name|string|**是**||provider/consumer|方法名称（不支持重载方法）|
|method|timeout|string|否||consumer|方法调用超时时间，单位毫秒|
|method|retries|int|否||consumer|方法重试次数（0表示失败后不重试）|
|method|validation|boolean|否||provider/consumer|是否校验参数，支持JSR303|
|method|concurrency|int|否|0|provider/consumer|**该方法**的最大可并行执行请求数|
|method|cache|boolean|否||provider/consumer|是否开启结果缓存。如果开启需要指定cacheProvider|
|method|compress|string|否||provider/consumer|压缩算法（启动后是否压缩还取决于数据包大小）|
|method|dstParam|int|否||consumer|目标参数（机房/分组等）索引，从0开始计数|
|method|cacheProvider|string|否|caffeine|provider/consumer|自定义结果缓存插件名称，配合cache属性实现开启关闭：caffeine、guava|
|method|cacheKeyGenerator|string|否|default|provider/consumer|cache key生成器名称|
|method|cacheExpireTime|long|否|-1|provider/consumer|cache过期时间，单位ms 毫秒|
|method|cacheNullable|boolean|否|false|provider/consumer|结果缓存值是否可空|
|method|cacheCapacity|int|否|10000|provider/consumer|结果缓存容量大小|

>二级元素：可以出现在provider、consumer标签下，下面可以有parameter节点。对应io.joyrpc.config.MethodConfig
用于配置方法级的一些属性，覆盖接口级的属性

```xml
<!-- 注册中心 -->
<joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
<!-- server配置 -->
<joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
<!-- consumer配置 -->
<joyrpc:consumer id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" registry="joyRpcRegistry">
  <!-- method配置 -->
  <joyrpc:method name="echo" timeout="3000"></joyrpc:method>
</joyrpc:consumer>
```
#### 参数配置
标签：`<joyrpc:param>` 

|标签|属性|类型|必填|默认值|描述|
| -- | --| -- | -- | :- | --- |
|parameter|key|string|**是**||参数配置关键字|
|parameter|value|string|**是**||参数配置值|
|parameter|hide|boolean|否|false|是否为隐藏配置。是的话，key自动加上"."作为前缀，且业务代码不能获取到，只能从filter里取到|

>作为一级元素：直接出现在spring的beans标签下。
作为二级元素：可以出现在registry、server、provider、consumer任一标签下。
或者三级元素，还可以出现在method下。
是一个key-value形式map。

```xml
<!-- 全局参数:一级元素 -->
<joyrpc:parameter key="payload" value="9000000" hide="true"/> 
<!-- 注册中心 -->
<joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory">
<!-- 参数:二级元素 -->
  <joyrpc:parameter key="app" value="testsafapp" hide="true"/>
</joyrpc:registry>
<!-- server配置 -->
<joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
<!-- provider配置 -->
<joyrpc:provider id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" ref="demoServiceImpl" server="myJoy">
  <!-- 接口级参数:二级元素 -->
  <joyrpc:parameter key="token" value="1q2w3e4r" hide="true"/> 
  <joyrpc:method name="echo" retries="2">
    <!-- 方法级参数:三级元素 -->
    <joyrpc:parameter key="bbb" value="bbbbbbb" hide="true"/> 
  </joyrpc:method>
</joyrpc:provider>
```

>常用全局配置参数

|标签|属性|类型|默认值|描述|
| -- | --| -- | :- | --- |
|parameter|connectTimeout|int|5000|创建连接的超时时间，单位毫秒|
|parameter|reconnectInterval|int|2000|客户端重连死亡服务端的间隔，单位毫秒。配置小于0代表不重连|
|parameter|payload|int|8388608|允许数据包大小|
|parameter|highWaterMark|int|65536|Netty通信通道高水位值，最大值不能超过Integer.MAX_VALUE|
|parameter|lowWaterMark|int|32768|Netty通信通道低水位值，其值不能超过高水位值，最小值不能低于8192|

#### 消费者组配置
标签：`<joyrpc:consumerGroup>` 

|标签|属性|类型|必填|默认值|描述|
| -- | --| -- | -- | :- | --- |
|consumerGroup|dstParam|int|否||目标参数（机房/分组等）索引，从0开始计数|
|consumerGroup|aliasAdaptive|boolean|否|false|是否自动适配alias，设为true当没有alias时自动引入|
|consumerGroup|groupRouter|string|否||自定义分组路由规则实现类|

>一级元素，对应io.joyrpc.config.ConsumerGroupConfig
 设置一个可以包含多个alias的客户端，调用哪个alias可以切换。

