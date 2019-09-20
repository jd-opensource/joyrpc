配置参考手册
===

- 引入joyrpc的schema，参考如下
````xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:joyrpc="http://joyrpc.io/schema/joyrpc"
       xsi:schemaLocation=
       "http://www.springframework.org/schema/beans        
       http://www.springframework.org/schema/beans/spring-beans.xsd
       http://joyrpc.io/schema/joyrpc  
       http://joyrpc.io/schema/joyrpc/joyrpc.xsd">
</beans>
````
#### 注册中心配置 
标签：`<joyrpc:registry>`

|标签|属性|类型|必填|默认值|描述|
| -- | --| -- | -- | -- | -- |
|registry|id|string|是||Spring的BeanId|
|registry|address|string|是||注册中心地址|
|registry|timeout|int|是|5000|调用注册中心超时时间，单位毫秒|
````
1.一级元素，下面可以有parameter节点。对应io.joyrpc.config.RegistryConfig
2.配置注册中心用，注册服务和订阅服务，订阅配置。 全局唯一即可。
````

#### 服务提供方配置  
标签：`<joyrpc:server>` 

|标签|属性|类型|必填|默认值|描述|
| -- | --| -- | -- | -- | -- |
|server|id|string|是||Spring的BeanId|
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
````
1.一级元素，下面可以有parameter节点。对应io.joyrpc.config.ServerConfig
2.配置服务端用，只在发布服务端时候声明。
3.默认为joyrpc协议，不需要再设置。
4.一个server下可以发布多个provider。
````

#### 消费者配置

#### 方法配置

#### 参数配置

#### 消费者组配置

