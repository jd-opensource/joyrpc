方法限制
==
支持一个接口下只发布部分方法，不推荐这样发布，如果不让调用最好从接口里删除该方法
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

### 1. XML方式

  配置接口下的include白名单和exclude黑名单即可，多个方法用英文逗号隔开。
  
  ```xml
  <beans>
      <joyrpc:provider id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" ref="demoServiceImpl" server="myJoy" include="*" exclude="sayHello"></joyrpc:provider>
  </beans>
  ```
### 2. API方式

  ```java
  providerConfig.setInclude("*");
  providerConfig.setExclude("sayHello");
  ```
