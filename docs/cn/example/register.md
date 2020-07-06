注册&订阅
==

1.注册同订阅互相独立、不互斥、不依赖，任意一个设置为true，则同注册中心建立长连接；两个均是false时，同注册中心不创建长连接。

2.在启动Provider时候，会自动去指定注册中心注册一条Provider信息，同时订阅接口配置。

>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示 JOYRPC 中的schema。

- 默认配置

  >默认所有provider及consumer均开启注册及订阅; 

  ```xml
  <beans>

    <joyrpc:registry id="joyrpcRegistry" registry="joyrpc"/>
  
    <joyrpc:provider />
  
    <joyrpc:consumer />
  
  </beans>
  
  ```

- 不注册及不订阅

  >（不推荐）可以通过如下配置实现不注册和不订阅。通常用于直连场景或线下调试。

  ```xml
    <beans>
  
      <joyrpc:registry id="joyrpcRegistry" registry="joyrpc"/>
    
      <joyrpc:provider register="false" subscribe="false" />
    
      <joyrpc:consumer register="false" subscribe="false" />
    </beans>
  ```