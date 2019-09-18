# 版本说明

## 1.0.2-SNAPSHOT

- ConsumerConfig增加无参异步refer方法，方便api方式调用

- 把boot里面部分类挪到spring，为了支持spring annotation

- 重命名MapParametic为MapParametric

- 每次调用完后，用户线程生成新的上下文，避免上下文被用户线程和IO线程共享

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

