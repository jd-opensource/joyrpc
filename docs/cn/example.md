使用示例
===
[TOC]


#### 注册

默认所有provider及consumer均开启注册及订阅; 
注册同订阅互相独立，不互斥，不依赖，任意一个设置为true，则同注册中心建立长连接。两个均是false时，同注册中心不创建长连接
````xml
<joyrpc:registry id="jsfRegistry" registry="joyrpc"/>
````
（不推荐）可以通过如下配置实现不注册和不订阅。
````xml
<joyrpc:provider register="false" subscribe="false" />
<joyrpc:consumer register="false" subscribe="false" />
````
在启动Provider时候，会自动去指定注册中心注册一条Provider信息，同时订阅接口配置
#### 订阅
#### 多注册中心
#### 指定发布IP
#### 指定端口
#### 集群策略
#### 负载均衡
#### 路由配置
#### 直连调用
#### 泛化调用
#### 超时时间
#### 隐式传参
#### 参数校验
#### Token调用
#### 调用压缩
#### 方法限制
#### 代理类配置
#### 延迟启动
#### 粘滞连接
#### 并发控制
#### 黑白名单
#### 数据包大小设置
#### 预热接口