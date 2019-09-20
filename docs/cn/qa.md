常见问题
===

Q: JOYRPC 内部是用 Zookeeper 作为注册中心的吗？可以集成其它 etcd 等注册中心吗？

   JOYRPC 的注册中心模块是可扩展的，对内对外使用的都是一套核心接口。目前开源的版本中集成了 Zookeeper、ETCD，其它的注册中心实现社区已经在集成中。

Q: 与Dubbo对比？
