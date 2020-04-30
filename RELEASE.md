# 版本说明

## 1.0.6-SNAPSHOT

### Enhancement

- 消费者调用，通过缓存的参数类型，加快构造Invocation的性能

- 增加JCompiler插件，避免系统没有tools.jar的时候编译报错

- 增加Consul注册中心支持

- 过载异常和限流异常不输出堆栈，减少CPU消耗

- 增加JdkGrpcFactory

- 优化GRPC获取GrpcType和构建方法参数的性能

### Bugfixes

- ETCD注册中心断开连接后，连接标识没有清空，造成重连可能不成功问题

- 网络异常消息配置改成小写，因为内部使用小写判断

## 1.0.5-RELEASE(2020-04-14)

### Enhancement

- 使用时间轮来替换定时调度逻辑

- Bootstrap

  - 增加Export注解，用于控制方法暴露输出
  
  - 消费者启动默认等待建连不超时
  
  - 如果服务提供者没有配置服务的IP，则起来绑定的地址为0.0.0.0
  
  - 预热接口改成异步，并且在Exporter的open方法做
  
  - 采用JSR303来进行配置验证
  
  - 配置只支持全量更新
  
  - 调整了GenericService的方法，泛化的接口可以是GenericService的子类，便于兼容历史版本
  
  - 增加了处理链构造器扩展点FilterChainFactory
  
  - 优化cluster初始化连接逻辑，若check设置为false（默认值改为true），则不用等待初始化连接成功，建连操作执行完后就ready
  
  - 接口类型验证时候，不推荐的类型只会输出一次。
  
  - 环境变量全部加载，并支持变量重命名
  
  - 全局上下文配置文件，支持环境变量定义，只有在配置文件中定义的环境变量才放入到全局上下文中
  
  - 增加表达式缓存键插件，并在方法配置里面增加缓存键表达式配置，默认提供了在Spring环境的SpEL支持
  
  - 完善重试策略的配置参数
  
  - 集群分发策略支持在方法上配置，便于读写方法不同的策略
  
- Registry

  - 去掉锁
  
  - 备份增加时间间隔，默认10秒，减少网关这样大规模集群备份的CPU占用
  
  - 增加是否启用备份的参数，同时其url增加了全局的静态配置信息
  
- Springboot

  - 处理启动时候的几个Warn信息
  
  - 增加分组调用的支持
  
- Cluster

  - 节点过滤掉不支持的客户端协议，避免连接错误，并且定期尝试
  
  - 添加initConnectTimeout配置，初始化连接超时时间，从第一次接收到集群连接连接事件开始计算，默认值15s
  
  - initTimeout配置默认值由0改为90000ms
  
- 序列化

  - 支持黑名单动态配置
  
- 处理链

  - 服务提供者在收到调用请求，如果本地没有请求指定的服务抛出ShutdownExecption，让客户端主动断开连接
  
  - 重试，切换节点，考虑到不同的节点协议不同，先删除原有节点协议注入的隐式参数
  
  - 优化自适应负载均衡的默认TP评分的分值区间算法
  
  - 优化参数验证过滤链，如果方法没有验证注解，则提前设置为不需要验证，加快性能
  
  - 优化限流性能，根据方法、分组和应用缓存每次匹配的最佳限流器 
  
  - Java8的CompletableFuture.get()内部会首先自旋，同步调用获取结果传入超时时间避免自旋，提升性能
  
  - 支持Java8接口上的默认方法和静态方法调用
  
  - 增加处理链工厂类，把处理链的验证委托给处理链工厂类，因为不同的实现会加载不同的插件
  
  - 在异步调用场景，调用结束也可能在调用主线程，请求上下文不自动恢复，由用户来进行保存。
    
  - 完善上下文透传问题，增加原生调用链支持，服务端A收到客户端B请求后，再调用C和D的时候，只会透传A设置的参数和B携带的调用链参数。另外在A的处理链中，B请求的参数是一致可见的，不会因为调用C后丢失，造成调用D没有携带调用链参数。

  - 完善身份认证、鉴权体系和相关插件
  
  - 增加广播模式和并行调用两种集群分发策略
  
  - 处理链需要的方法参数都合并到了MethodOption，在exporter和refer里面提前绑定好了，并设置到request里面。加快性能

  - 优化自适应负载均衡性能
  
  - 优化回调方法性能
  
  - 优化codec的数据包写入性能
  
  - 预编译，动态生成方法调用的Java类，取代反射调用，优化性能

- 工具类
    
  - ClassUtils的getPublicMethods返回公共的方法，包括静态方法，支持Java8接口上的静态方法调用，调整获取getter和setter方法，必须有对应的字段  
  
- Dependency

  - 升级Springboot为2.2.1.RELEASE版本

  - 升级Spring为5.2.1.RELEASE版本
  
  - 升级Hazelcast为3.12.5版本
  
  - 升级Protostuff为1.6.2版本，支持Object[]元素为null。
  
  - 升级fastjson为1.2.68版本
  
  - 升级bytebuddy为1.10.9版本
  
  - 升级javassist为3.27.0-GA版本

### Bugfixes

- Cluster

  - 完全去掉Cluster和Node的锁，防止死锁
    
  - 节点的指标事件返回准确的Transport的请求数(正在发送+待应答），而不是Channel的待应答请求数
  
  - 获取节点客户端协议的时候以URL为Key缓存，缓存无意义，改成使用协议版本和名称作为Key缓存
  
  - 节点连接断开，在注册中心集群事件先到的情况下，可能造成没有从就绪节点里面删除掉。

- 注册中心

  - 事件执行过快，条件没满足 
 
- 处理链

  - 自适应负载均衡当都评估不可用的时候，没有进行选择直接返回空，造成调用出错。
  
  - 重试请求，重试次数注入报错
  
  - ChannelBuffer的writeString方法，长度写入错误
    
- Bootstrap

  - ConsumerConfig和ProviderConfig移除配置监听器方法，没有正确移除
  
  - 修改在接口上使用@Alias注解，消费者不能正确的调用和拿不到配置的问题。会使用Alias配置的名称替换ConsumerConfig配置的接口名称
  
  - 修复Javassist代理在获取数组类型的名称错误问题
  
  - 应用同时提供服务和该服务的消费者，如果只有一个实例的情况下并且消费者的check参数默认为false，优雅启动会等待连接服务超时启不起来。
  
  - 认证消息类型错误造成无法认证通过
  
  - 分组调用标识参数注入验证报错的问题
  
- HTTP

  - 对Header的值进行URL解码处理中文乱码
  
- GRPC

  - 对Header的值进行URL解码处理中文乱码
  
  - 应答包装类参数传递有误
  
- 序列化

  - 修复Fastjson序列化Invocation的异常
  
- Proxy

  - javassist基本类型报错问题
  
  - javassist包装静态方法报错，过滤掉静态方法
  
- 工具类
     
  - 修复ClassUtils中的isWriteable方法，final修饰的field仍判定可写的问题
  
  - JdkHttpClient可能读取数据不完整

## 1.0.4-SNAPSHOT

### New Features  

- Springboot支持第三方服务提供者和消费者注解插件

- 增加第三方配置中心注入

### Enhancement

- Bootstrap

  - ProviderConfig增加exportAndOpen方法，便于API调用
  
  - AbstractConsumerConfig增加proxy方法，方便在spring场景提前创建好代理对象

- Cluster

  - 优化区域感知算法，跨机房选择，优先根据首选配置选择本区域首选，再选择本区域，然后再选择其它区域首选。跨机房首选机房配置放在全局配置里面。如果没有指定候选者最小数量并且允许跨机房选择，则尝试获取就近机房节点数量平均值和本地机房的数量的最大值作为选择的目标，防止本地机房节点数量很少的情况

- 注册中心

  - 优化Broadcast注册中心，数据备份默认改成同步和异步各一份，读主节点。当Provider实例停止的时候，Consumer能快速去掉节点
  
- Spring&Springboot

  - 完善Spring和SpringBoot支持，Spring只支持xml解析，Springboot支持注解方式，并支持消费者和生产者参数在配置文件中配置。
  
- 序列化
  
  - 修改泛化调用的反序列化逻辑，优先按照用户传递的子类参数类型来进行反序列化
  
  - grpc请求包装类增加接口类名作为前缀名称
  
- 工具类
  
  - ClassUtils的getPublicMethods返回公共的非静态方法

- Dependency

  - 升级netty为4.1.48.Final版本

  - 升级hazelcast为3.12.4版本

  - 升级commons-compress为1.19，解决安全漏洞
  
  - 升级hibernate-validator为6.0.18.Final

### Bugfixes

- Bootstrap

  - 修复优雅停机问题，Shutdown没有正确的触发对象close方法产生的CompletableFuture事件

- 注册中心&动态配置

  - 注册中心在持久化数据的时候潜在的空指针问题

  - 修改ETCD续约成功，连续续约失败次数没有重置的问题

  - 修复ETCD和ZK注册中心，初始化时候，当注册中心集群连接不上，后续集群恢复后，无法正常重连问题

  - ZK注册中心添加连接监听，与ZK重连成功，触发recover

  - 修复ZK注册中心初始化时未成功连接到集群，后续连接到集群，却不重新发起服务订阅与注册的问题

  - 修改注册中心，集群事件增量数据先到达，全量数据后到达需要正确合并的问题
  
  - 修复配置动态更新的问题
  
  - 配置动态更新JSON解析异常捕获
  
- GRPC

  - 修改GrpcClientProtocol，每次build chain的时候都重新创建，防止内部逻辑发生StreamId冲突

  - GRPC存在潜在的内存泄漏问题，ByteBuf没有Release

- Spring&Springboot

  - 修改SpringBoot方式采用SpringLoader加载插件挂住的问题

  - 修复SpringBoot方式consumer调用refer为null的问题

  - Spring中的ConsumerBean初始化异常退出，当是泛型的时候正确返回对象类型

  - Spring中的ProviderBean初始化异常退出
  
  - 处理Spring的xml配置文件中全局参数占位符替换
  
  - 处理Spring和SpringBoot服务提供者在进程关闭的时候不能正常的Unrefer问题

- Transport

  - 修复transport层关于isWritable判断不合理，导致client不可读的问题
  
 - Telnet 
  
  - 修复invoke命令的密码设置问题

## 1.0.3-RELEASE(2019-10-12)

- 升级Fastjson版本为1.2.61，防止autoType漏洞

- 去掉joyrpc-extension-core中的Predicate，使用Java8原生的Predicate

- ChanelManager优化poolchannle关闭逻辑

- 修复机房信息获取错误

- 区域感知算法，增加跨机房调用的首选机房配置

- 优化心跳逻辑，如果channel为Inactive状态，直接推送Inactive事件

- 优化channel发送消息的异常处理

- 删除Cluster中的supply方法中的同步connect调用，在节点disconnect事件里面可能造成死锁

- 优化StandardValidator.MyConsumer.onCustomAbstract方法返回异常的message

## 1.0.2-RELEASE(2019-09-20)

- ConsumerConfig增加无参异步refer方法，方便api方式调用

- 把boot里面部分类挪到spring，为了支持spring annotation

- 重命名MapParametic为MapParametric

- 每次调用完后，用户线程生成新的上下文，避免上下文被用户线程和IO线程共享

- 升级bytebuddy版本为1.10.1

- 添加hazelcast组播方式注册中心实现

## 1.0.1-RELEASE(2019-09-11)

- 网络建连成功才注册Future超时回调函数，防止在构造函数里面注册，存在内存泄漏问题。同时优化了无请求时候的判断效率

- 去掉几个守护线程里面的CountdownLatch的await，改用Thread.sleep，减少CPU消耗

- 接口类型检查，GenericChecker存在相互引用造成递归检测堆栈溢出的问题，已经检测过的类型不需要检查

- 升级Fastjson的版本为1.2.60，防止DDos攻击

- 调大ClusterManager的线程池的coreSize

- Fastjson序列化Feature从GlobalContext获取

- 修复Protostuff序列化ZoneOffset问题，ZoneOffset.ofTotalSeconds(0)会被JVM缓存，每次调用返回同一个对象，有污染。改成ZoneOffset.ofTotalSeconds(1)每次创建新对象

- 网关泛化调用，如果结果为null，返回null

- Fastjson默认加上SerializerFeature.WriteNonStringKeyAsString;

- GlobalContext中的常量移动到Constants

## 1.0.0-RELEASE(2019-09-06)

- 发布正式版本

