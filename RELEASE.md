# 版本说明

## 1.0.4-SNAPSHOT

### New Features  

- Springboot支持第三方服务提供者和消费者注解插件

### Enhancement

- ProviderConfig增加exportAndOpen方法，便于API调用

- 配置动态更新JSON解析异常捕获

- 跨机房首选机房配置放在全局配置里面

- 优化区域感知算法，跨机房选择，优先根据首选配置选择本区域首选，再选择本区域，然后再选择其它区域首选。如果没有指定候选者最小数量并且允许跨机房选择，则尝试获取就近机房节点数量平均值和本地机房的数量的最大值作为选择的目标，防止本地机房节点数量很少的情况

- 升级netty为4.1.43.Final版本

- 升级hazelcast为3.12.4版本

- 升级commons-compress为1.19，解决安全漏洞

- 优化BroadCast注册中心，改成2个备份，当Provider实例停止的时候，Consumer能快速去掉节点

- 调整Spring和SpringBoot支持，Spring只支持xml解析，Springboot支持注解方式，并支持消费者和生产者参数在配置文件中配置。

- 修改泛化调用的反序列化，优先按照用户传递的子类参数类型来进行反序列化

- ClassUtils的getPublicMethods返回公共的非静态方法

### Bugfixes

- 修复优雅停机问题，Shutdown没有正确的触发对象close方法产生的CompletableFuture事件

- 注册中心在持久化数据的时候潜在的空指针问题

- 修改ETCD续约成功，连续续约失败次数没有重置的问题

- 修复ETCD和ZK注册中心，初始化时候，当注册中心集群连接不上，后续集群恢复后，无法正常重连问题

- ZK注册中心添加连接监听，与ZK重连成功，触发recover

- 修复ZK注册中心初始化时未成功连接到集群，后续连接到集群，却不重新发起服务订阅与注册的问题

- 修改注册中心，集群事件增量数据先到达，全量数据后到达需要正确合并的问题

- 修改GrpcClientProtocol，每次build chain的时候都重新创建，防止内部逻辑发生StreamId冲突

- GRPC存在潜在的内存泄漏问题，ByteBuf没有Release

- 修改SpringBoot方式采用SpringLoader加载插件挂住的问题

- 修复SpringBoot方式consumer调用refer为null的问题

- Spring中的ConsumerBean初始化异常退出，当是泛型的时候正确返回对象类型

- Spring中的ProviderBean初始化异常退出

- AbstractConsumerConfig增加proxy方法，方便在spring场景提前创建好代理对象

- 修复transport层关于isWritable判断不合理，导致client不可读的问题

- 处理Spring的xml配置文件中全局参数占位符替换

- 处理Spring和SpringBoot服务提供者在进程关闭的时候不能正常的Unrefer问题

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

