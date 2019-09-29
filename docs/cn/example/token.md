token调用
==
token认证基于隐式传参实现，Provider和Consumer需要指定同一个token才能进行调用，否则抛出异常。XML方式同API方式可以交叉使用。
>说明：下面示例中采用  **`<beans/>`** 标签 表示JOYRPC中的schema。

### Spring xml方式

- Provider配置如下：加入隐藏key：token
  
  ```xml
  <joyrpc:provider id="demoService" interface="io.joyprc.service.DemoService" alias="joyrpc-demo" ref="demoServiceImpl" server="myJoy">

      <joyrpc:parameter key="token" value="1qaz2wsx" hide="true" />
  </joyrpc:provider>
  ```
- Consumer配置如下：同样加入隐藏key：token

  ```xml
  <joyrpc:consumer id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo">

      <joyrpc:parameter key="token" value="1qaz2wsx" hide="true" />
  </joyrpc:consumer>
  ```
>双方一致才能完成调用，否则抛出异常。

### API方式

>API方式为io.joyrpc.constants#HIDDEN_KEY_TOKEN  就是 .token 前面带点

- Provider配置
  ```
  ProviderConfig.setParameter(Constants.HIDDEN_KEY_TOKEN,"1qaz2wsx");
  ```

- Consumer配置
  ```
  consumerConfig.setParameter(Constants.HIDDEN_KEY_TOKEN,"1qaz2wsx");
  ```