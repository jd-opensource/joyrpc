配置参考手册
===

- 引入joyrpc的schema，参考如下

  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <beans xmlns="http://www.springframework.org/schema/beans"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns:joyrpc="http://joyrpc.io/schema/joyrpc"
         xsi:schemaLocation=
         "http://www.springframework.org/schema/beans  http://www.springframework.org/schema/beans/spring-beans.xsd
         http://joyrpc.io/schema/joyrpc  
         http://joyrpc.io/schema/joyrpc/joyrpc.xsd">
  </beans>
  ```
  >说明：上面是完整的schema描述，下面示例中采用  **`<joyrpc/>`** 标签 代表此schema。
  
#### 注册中心配置 

  标签：`<joyrpc:registry>`

属性|类型|必填|默认值|描述|
| :----:| :----: | :----: | :----: | :----: |
|id|String|**是**| |Spring的BeanId|
|address|String|**是**| |注册中心地址|
|registry|String|**是**|memory|注册中心类型|

  >1.一级元素，下面可以有parameter节点。对应io.joyrpc.config.RegistryConfig
  2.配置注册中心用，注册服务和订阅服务，订阅配置。 全局唯一即可。

  ```xml
  <beans>
      <!-- 注册中心 -->
      <joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
  </beans>
  ```
  目前支持的注册中心类型如下：
  
  |类型|名称|描述|
  | :----: | :----: | :----: | 
  |memory|内存|基于内存的注册中心，适合单节点，可用于测试|
  |broadcast|广播|广播模式|
  |consul|Consul|Consul注册中心|
  |etcd|ETCD|ETCD注册中心|
  |zk|Zookeeper|Zookeeper注册中心|

#### 服务server配置  

  标签：`<joyrpc:server>` 

属性|类型|必填|默认值|描述|
| :----: | :----: | :----: | :----: | :----: |
|id|String|**是**| |Spring的BeanId|
|host|String|否| |服务端绑定地址|
|port|int|否|22000|服务端绑定端口。如果端口被占用，会提示启动失败|
|contextPath|String|否| |发布上下文。用于基于http的协议。|
|coreThreads|int|否|20|业务线程池core线程数|
|maxThreads|int|否|int|业务线程池最大线程数|
|threadPool|String|否|adaptive|线程池插件名称|
|ioThreads|int|否|0|IO线程池大小，程序中默认max(8,cpu+1)|
|queues|int|否|0|业务线程池队列大小。0表示无队列，正整数表示有限队列|
|accepts|int|否|2147483647|允许的TCP长连接数（包括http），不能填写小于0的值|
|buffers|int|否|8192|IO的缓冲区大小，最大：32768，最小：1024|
|epoll|int|否|false|Linux下是否启动epoll特性|
|queueType|String|否|normal|业务线程池队列类型|

  >1.一级元素，下面可以有parameter节点。对应io.joyrpc.config.ServerConfig
  2.配置服务端用，只在发布服务端时候声明。
  3.默认为joyrpc协议，不需要再设置。
  4.一个server下可以发布多个provider。

  ```xml
  <beans>
  <!-- 注册中心 -->
    <joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
    <!-- server配置 -->
    <joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
  </beans>
  ```

#### 服务提供方配置

  标签：`<joyrpc:provider>` 

属性|类型|必填|默认值|描述|
| :----: | :----: | :----: | :---- | :----  |
|id|String|**是**| |Spring的BeanId|
|interface|String|**是**| |发布的接口名称|
|alias|String|**是**| |服务别名分组信息|
|ref|ref|**是**| |接口实现类|
|ref|**是**|全部server|需要发布服务到的Server，一个server对应一个端口|
|filter|ref|否| |自定义过滤器实现插件名称|
|ref|否| |注册中心引用，多个用英文逗号隔开|
|register|Boolean|否|true|是否注册到注册中心|
|subscribe|Boolean|否|true|是否从注册中心订阅|
|timeout|Int|否|5000|服务端调用超时时间，单位毫秒。|
|proxy|String|否|bytebuddy|代理类, 插件名称：bytebuddy、javassist、jdk|
|cache|Boolean|否| |是否开启结果缓存。如果开启需要指定cacheProvider|
|cacheProvider|String|否|caffeine|缓存插件名称： caffeine、guava|
|cacheKeyGenerator|String|否|json|cache key生成器名称|
|cacheExpireTime|long|否|-1|cache过期时间，单位ms 毫秒|
|cacheNullable|Boolean|否|false|结果缓存值是否可空|
|cacheCapacity|int|否|10000|结果缓存容量大小|
|delay|int|否|0|延迟发布服务时间。|
|weight|int|否|100|服务提供者权重|
|include|String|否|*|发布的方法列表，逗号分隔|
|exclude|String|否| |不发布的方法列表，逗号分隔|
|concurrency|int|否|-1|接口下每方法的最大可并行执行请求数, -1: 关闭，0: 开启但不限制|
|validation|Boolean|否|false|是否校验参数|
|compress|String|否|lz4|压缩算法插件 lz4、snappy、lzma、zlib|
|interfaceValidator|String|否|standard|接口验证器插件名称，同validation参数配合使用|
|warmup|String|否|standard|预热插件名称|

  >一级元素，下面可以有method或者parameter节点。对应io.rpc.config.ProviderConfig
  >发布joyrpc服务Provider使用。 

  ```xml
  <beans>
      <!-- 注册中心 -->
      <joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
      <!-- server配置 -->
      <joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
      <!-- 接口实现配置 -->
      <bean id="demoServiceImpl" class="io.joyrpc.service.impl.DemoServiceImpl"/>
      <!-- provider配置 -->
      <joyrpc:provider id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" ref="demoServiceImpl" server="myJoy"/>
  </beans>
  ```

#### 消费者配置

  标签：`<joyrpc:consumer>` 

|属性|类型|必填|默认值|描述|
| :----: | :----: | :----: | :---- | :----  |
|id|String|**是**| |Spring的BeanId|
|interface|String|**是**| |调用的接口名称|
|alias|String|**是**| |服务别名分组信息|
|url|String|否| |直连地址，配置了此地址就不再从注册中心获取|
|filter|String|否| |过滤器插件名称，多个用英文逗号隔开|
|ref|**是**| |注册中心配置引用 |
|register|Boolean|否|true|是否注册到注册中心|
|subscribe|Boolean|否|true|是否从注册中心订阅|
|timeout|int|否|5000|调用端调用超时时间，单位毫秒|
|proxy|String|否|bytebuddy|代理类生成方式插件配置，插件名称：bytebuddy、javassist、jdk|
|cache|Boolean|否|false|是否开启结果缓存。如果开启需要指定cacheProvider|
|cacheProvider|String|否|caffeine|自定义结果缓存插件名称：caffeine、guava|
|cacheKeyGenerator|String|否|default|cache key生成器名称|
|cacheExpireTime|long|否|-1|cache过期时间，单位ms 毫秒|
|cacheNullable|Boolean|否|false|结果缓存值是否可空|
|cacheCapacity|int|否|10000|结果缓存容量大小|
|generic|Boolean|否|false|是否泛化调用|
|cluster|String|否|failover|集群策略插件名称，已支持：failover、failfast、pinpoint、broadcast和forking 方式|
|retries|int|否|0（0表示失败后不重试）|失败后重试次数（需要和cluster=failover结合使用，单实例设置retries无效）|
|retryOnlyOncePerNode|Boolean|否| |每个节点只调用一次 |
|failoverWhenThrowable|String|否| | 可以重试的异常全路径类名，多个用逗号分隔 |
|failoverPredication|String|否| | 重试异常判断接口插件 |
|failoverSelector|String|否| |  异常重试目标节点选择器 |
|loadbalance|String|否|randomWeight|负载均衡算法插件名称：roundRobin、randomWeight、adaptive|
|sticky|Boolean|否|false|是否粘滞连接（除非断开连接，只调一个）|
|injvm|Boolean|否|true|是否走injvm调用（如果同一jvm内发布了服务，则不走远程调用）|
|check|Boolean|否|false|是否强依赖服务端（无可用服务端启动失败）|
|serialization|String|否|hessian|序列化插件名称：protostuff、msgpack、json@fastjson、fst、kryo、hessian、java，选用protostuff 性能更高，使用时需要注意数组、集合中不能有null元素|
|nodeSelector|String|否| |目标节点选择器插件名称，已支持 methodRouter(基于方法参数的路由)|
|concurrency|int|否|-1|接口下**每方法**的最大可并行执行请求数，配置-1关闭并发过滤器，等于0表示开启过滤但是不限制|
|validation|Boolean|否|false|是否校验参数|
|compress|String|否|lz4|压缩算法插件名称：lz4、snappy、lzma、zlib|
|interfaceValidator|String|否|standard|接口验证器插件名称，同validation参数配合使用|
|initSize|int|否|10|初始化连接数|
|minSize|int|否|0|最小连接数|
|candidature|String|否|""|候选者算法插件|
|warmupWeight|int|否|0|预热权重，默认取consumer端用户设置；用户未设置时，取provider端设置的权重|
|warmupDuration|int|否|60000|单位时间内预热权重到达100，默认60000毫秒（即1分钟）|

  >作为一级元素，下面可以有method或者parameter节点，对应io.joyrpc.config.ConsumerConfig。
  作为二级元素，ConsumerGroup下的元素。
  发布joyrpc服务Consumer使用。

  ```xml
  <beans>
      <!-- 注册中心 -->
      <joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
      <!-- server配置 -->
      <joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
      <!-- consumer配置 -->
      <joyrpc:consumer id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" registry="joyRpcRegistry"/>
  </beans>
  ```

#### 方法配置

  标签：`<joyrpc:method>` 

|属性|类型|必填|默认值|描述|
| :----: | :----:| :----: | :----: | :----: |
|name|String|**是**| |方法名称（不支持重载方法）|
|timeout|String|否| |方法调用超时时间，单位毫秒|
|retries|int|否| |方法重试次数（0表示失败后不重试）|
|retryOnlyOncePerNode|Boolean|否| |每个节点只调用一次 |
|failoverWhenThrowable|String|否| | 可以重试的异常全路径类名，多个用逗号分隔 |
|failoverPredication|String|否| | 重试异常判断接口插件 |
|failoverSelector|String|否| |  异常重试目标节点选择器 |
|validation|Boolean|否| |是否校验参数，支持JSR303|
|concurrency|int|否|0|**该方法**的最大可并行执行请求数|
|compress|String|否| |压缩算法（启动后是否压缩还取决于数据包大小）|
|dstParam|int|否| |目标参数（机房/分组等）索引，从0开始计数|
|cache|Boolean|否| false |是否开启结果缓存。如果开启需要指定cacheProvider|
|cacheProvider|String|否|caffeine|结果缓存插件名称，默认提供了caffeine、guava和map缓存插件，需要引用相关的类库才能启用|
|cacheKeyGenerator|String|否|json|缓存键生成器名称，系统内置了json和Spring环境下的spel表达式生成器|
|cacheExpireTime|long|否|-1|cache过期时间，单位ms 毫秒|
|cacheNullable|Boolean|否|false|结果缓存值是否可空|
|cacheCapacity|int|否|10000|结果缓存容量大小|
|cacheKeyExpression|String|否| |缓存键表达式，用于表达式缓存键生成器，如spel|

  >二级元素：可以出现在provider、consumer标签下，下面可以有parameter节点。对应io.joyrpc.config.MethodConfig
  用于配置方法级的一些属性，覆盖接口级的属性

  ```xml
  <beans>
      <!-- 注册中心 -->
      <joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
      <!-- server配置 -->
      <joyrpc:server id="myJoy" port="22000" coreThreads="20" maxThreads="200"/>
      <!-- consumer配置 -->
      <joyrpc:consumer id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" registry="joyRpcRegistry">
        <!-- method配置 -->
        <joyrpc:method name="echo" timeout="3000"/>
      </joyrpc:consumer>
  </beans>
  ```
#### 参数配置

  标签：`<joyrpc:parameter>` 

|属性|类型|必填|默认值|描述|
| :----: | :----: | :----: | :----: | :----: |
|key|String|**是**| |参数配置关键字|
|value|String|**是**| |参数配置值|
|hide|Boolean|否|false|是否为隐藏配置。是的话，key自动加上"."作为前缀，且业务代码不能获取到，只能从filter里取到|

  >作为一级元素：直接出现在spring的beans标签下。
  作为二级元素：可以出现在registry、server、provider、consumer任一标签下。
  或者三级元素，还可以出现在method下。
  是一个key-value形式map。

  ```xml
  <bean>
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
  </bean>
  ```

  >常用全局配置参数

属性|类型|默认值|描述|
| :----: | :----: | :---- | :----: |
|connectTimeout|int|5000|创建连接的超时时间，单位毫秒|
|reconnectInterval|int|2000|客户端重连死亡服务端的间隔，单位毫秒。配置小于0代表不重连|
|payload|int|8388608|允许数据包大小|
|highWaterMark|int|65536|Netty通信通道高水位值，最大值不能超过Integer.MAX_VALUE|
|lowWaterMark|int|32768|Netty通信通道低水位值，其值不能超过高水位值，最小值不能低于8192|

#### 消费者组配置

  标签：`<joyrpc:consumerGroup>` 

属性|类型|必填|默认值|描述|
| :----: | :----: | :----: | :---- | :----: |
|dstParam|int|否| |目标参数（机房/分组等）索引，从0开始计数|
|aliasAdaptive|Boolean|否|false|是否自动适配alias，设为true当没有alias时自动引入|
|groupRouter|String|否| |自定义分组路由规则实现类|

  >一级元素，对应io.joyrpc.config.ConsumerGroupConfig
   设置一个可以包含多个alias的客户端，调用哪个alias可以切换。

