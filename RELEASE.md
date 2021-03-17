# 版本说明

## 1.4.8-SNAPSHOT

### Bugfixes

- 修复对返回值为基本类型方法通过上下文设置异步后报错问题，根据基本类型返回其初始值。

## 1.4.7-RELEASE(2021-03-09)

### Enhancement

- 重构线程池接口
  
- 重构网络模型接口

### Bugfixes

- 修复集群对客户端事件异步处理逻辑没有进行二次状态判断，可能造成误操作问题

- 修复节点打开后前置条件没有清理的问题

- 修复集群关闭方法里面没有调用所有节点的关闭

- 修复以应用服务名称注册，当服务实例重启后可能造成客户端自动关闭和该服务实例的建好的连接

- 修复HTTP2流ID溢出没有重连问题

- 修复GRPC业务处理异常没有正确输出问题

## 1.4.6-RELEASE(2021-02-19)

## Bugfixes

- 修复共享连接重复添加心跳任务

## 1.4.5-RELEASE(2021-02-04)

### Enhancement

- 增加无效节点过滤器插件，默认过滤无效客户端协议和SSL不匹配的节点

- 节点选择器增加采样选择器，默认采样100个节点，避免负载均衡算法大量节点造成的CPU消耗

- 节点选择器默认为采样选择器，可以配置多个节点选择器，用逗号分隔，依次进行节点筛选

- 优化代码，完善状态机

- Dependency

    - 升级Bytebuddy为1.10.20版本

    - 升级Hazelcast为4.1.1版本

    - 升级nacos为1.4.1版本

## Bugfixes

- 修复连接通道处理器的inactive不能正常触发的问题

- 修复服务端下线后短时间内还继续往该节点发送的问题，当收到服务端下线通知时候提前从就绪节点列表中删除

- 修复以应用服务名称来进行注册的时候，存在服务端关闭注销不完整的问题

## 1.4.4-RELEASE(2021-01-09)

### Enhancement

### Bugfixes

- 手动设置请求超时的Key从.timeout改为timeout，便于兼容

- ConumserConfig的refer出现异常先unrefer，再触发其他事件

## 1.4.3-RELEASE(2020-12-08)

### Enhancement

- 进程关闭状态不再发送心跳

- Dependency

    - 升级Skywalking为8.3.0版本

### Bugfixes

- 修改Springboot配置的插件加载不了的问题

## 1.4.2-RELEASE(2020-11-30)

### Enhancement

- nacos 1.4.0支持，支持配置更新

- 完善consul，注册的服务名称默认增加分组信息，可以在注册中心URL上配置consul.serviceWithGroup=false来禁用

- Broadcast模式，支持配置广播地址参数

- Dependency

    - 升级Fastjson为1.2.75版本

### Bugfixes

- 修复两个完全配置一下的消费者在启动后创建了不同的对象引用的问题

- 修改Springboot配置的插件可能加载不了的问题

- 修复GenericService实例调用非$invoke方法时有数组越界异常的问题

## 1.4.1-RELEASE(2020-11-05)

### Bugfixes

- 修复工程手动引入hessian包后，内置的hessian加载序列化扩展冲突问题

## 1.4.0-RELEASE(2020-10-20)

### Enhancement

- 增加json-rpc 2.0协议支持

- http协议和grpc协议服务端增加http头参数注入插件，便于自定义参数注入

### Bugfixes

- 修复注册中心的内部任务调度，在某些情况下添加了任务任然需要10秒才调度的问题

## 1.3.0-RELEASE(2020-09-21)

### Enhancement

- 添加分布式事务seata的集成

- 增加ipv6支持，目前内置的广播服务发现已经支持ipv6

- 增加HttpController插件

- 动态分组增加自定义分组配置参数

- Dependency

    - 升级Springboot为2.2.10.RELEASE版本

    - 升级Spring为5.2.9.RELEASE版本

    - 升级vertx-consul-client为3.9.3版本

### Bugfixes

- 处理在Springboot的某些场景下，接口验证中判断引用对象是否是目标接口返回错误的问题

## 1.2.0-RELEASE(2020-08-26)

### Enhancement

- 添加序列化白名单功能，默认开启白名单和关闭Fastjson的安全模式

- 增加Jackson序列化

### Bugfixes

- 修复消费者对gRPC处理的问题，把bizMsgId改成long型

## 1.1.0-RELEASE(2020-07-03)

### Enhancement

- 性能优化

    - 消费者调用，通过缓存的参数类型，加快构造Invocation的性能

    - 使用线程变量优化Hessian性能

    - 优化GRPC获取GrpcType和构建方法参数的性能

    - 过载异常和限流异常不输出堆栈，减少CPU消耗

    - 客户端增加TCP_NODELAY设置

    - 优化获取方法熔断器的性能

- 增加JCompiler插件，避免系统没有tools.jar的时候编译报错

- 增加Consul注册中心支持

- 增加nacos注册中心集成

- 增加Dubbo协议支持，目前支持hessian2,kryo,fst,java,protostuff序列化协议。

- 增加分布式跟踪过滤器，并集成了jaeger和skywalking的实现

- 增加JdkGrpcFactory

- 增加以服务名称而不是接口进行发布订阅，以便于更好的融入云原生

- Springboot配置文件中对全局参数通过"ref:"前缀实现对象引用配置

- 熔断的判断条件，连续失败次数和可用率可以并存，默认加上服务端过载和超时异常，避免对所有异常进行熔断

- Fastjson默认开启安全模式，使用@type标签会抛异常。可以在环境变量或Springboot配置文件中设置fastjson.parser.safeMode=false来关闭安全模式

- 增强泛型检测，在json序列化或泛化调用的时候，服务端尽可能地识别出泛型信息，正常地反序列化，而不需要传递额外的类型

- 增加回调功能，回调参数不必是Callback接口，可以在回调参数上使用注解@CallbackArg，或者通过消费者方法上配置的的callbackArg指明第几个参数是回调参数

- 默认服务端序列化方式优先，消费者优先从注册中心返回的服务端URL获取序列化方式

- Dependency

    - 升级caffeine为2.8.2版本

    - 升级fastjson为1.2.70版本，解决安全漏洞

### Bugfixes

- ETCD注册中心断开连接后，连接标识没有清空，造成重连可能不成功问题

- 网络异常消息配置改成小写，因为内部使用小写判断

- 修复protostuff&protobuf的序列化没有使用上自定义schema和protobuf反序列化异常

- 修复grpcFactory没有对枚举和数组生成包装器的问题

- 修复Bytebudy代理不能正确包装父接口的方法问题

- 当子类重新定义父类的字段，Hessian不能正确反序列化，处理方式保持和dubbo兼容

- 平滑限流器生效的时候初始化给最大限流数，防止生效的时刻拒绝所有请求

- Spring中的消费者和服务提供者在全局上下文初始化Bean完之前就启动了

- 修复应用启动多个Spring ApplicationContext，启动出错问题。

- 修复Telnet命令invoke的分组参数识别不了的问题

- 修复本地调用请求上下文没有正确注入的问题，以及本地调用缺乏超时时间控制问题

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

    -
    优化区域感知算法，跨机房选择，优先根据首选配置选择本区域首选，再选择本区域，然后再选择其它区域首选。跨机房首选机房配置放在全局配置里面。如果没有指定候选者最小数量并且允许跨机房选择，则尝试获取就近机房节点数量平均值和本地机房的数量的最大值作为选择的目标，防止本地机房节点数量很少的情况

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

