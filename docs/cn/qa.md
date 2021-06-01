常见问题
===

Q: JOYRPC 内部是用 Zookeeper 作为注册中心的吗？可以集成其它 ETCD 等注册中心吗？

>  JOYRPC 的注册中心模块是可扩展的，对内对外使用的都是一套核心接口。目前开源的版本中集成了 Zookeeper、ETCD，Hazelcast，其它的注册中心实现社区已经在集成中。
   
Q: IPV6支持？

>  根据当前jvm虚拟机的参数来选用IPV4或IPV6，在支持IPV6的机器上，可以配置系统参数java.net.forceIPv6Stack=true来强制使用IPV6，便于进行调试，系统内置的广播模式服务发现已经支持IPV6。
   
Q: 分布式事务支持？

>  支持Seata分布式事务

Q: 指定网卡支持？

>  当存在多个网卡的情况下，可以配置系统参数LOCAL_NIC={网卡名称}来强制使用IPV6可以通过设置
