# 版本说明

## 1.0.4-SNAPSHOT

- ProviderConfig增加exportAndOpen方法，便于API调用

- 配置动态更新JSON解析异常捕获

- 跨机房首选机房配置放在全局配置里面

- 跨机房选择，优先根据首选配置选择本区域首选，再选择本区域，然后再选择其它区域首选

- 注册中心在持久化数据的时候潜在的空指针问题

- 修改ETCD续约成功，连续续约失败次数没有重置的问题

- 修复ETCD和ZK注册中心，初始化时候，当注册中心集群连接不上，后续集群恢复后，无法正常重连问题

- 升级netty为4.1.42.Final版本

- ZK注册中心添加连接监听，与ZK重连成功，触发recover

- 修复ZK注册中心初始化时未成功连接到集群，后续连接到集群，却不重新发起服务订阅与注册的问题

- 修复优雅停机问题，Shutdown没有正确的触发对象close方法产生的CompletableFuture事件

- 优化BroadCast注册中心，改成2个备份，当Provider实例停止的时候，Consumer能快速去掉节点

## 1.0.3(2019-10-12)

- 升级Fastjson版本为1.2.61，防止autoType漏洞

- 去掉joyrpc-extension-core中的Predicate，使用Java8原生的Predicate

- ChanelManager优化poolchannle关闭逻辑

- 修复机房信息获取错误

- 区域感知算法，增加跨机房调用的首选机房配置

- 优化心跳逻辑，如果channel为Inactive状态，直接推送Inactive事件

- 优化channel发送消息的异常处理

- 删除Cluster中的supply方法中的同步connect调用，在节点disconnect事件里面可能造成死锁

- 优化StandardValidator.MyConsumer.onCustomAbstract方法返回异常的message

## 1.0.2(2019-09-20)

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

