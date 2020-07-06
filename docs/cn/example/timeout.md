超时时间
==
通常设置在Consumer端，可以指定接口级别，方法级别。
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

超时时间单位为ms（毫秒）

### 接口级别设置
  ```xml
  <beans>
  <!-- 超时时间1秒 -->
    <joyrpc:consumer id="demoService"  interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" timeout="1000"></joyrpc:consumer>
  </beans>
  ```

### 方法级别设置
  ```xml
  <beans>
      
    <joyrpc:consumer id="demoService"  interface="io.joyrpc.service.DemoService" alias="joyrpc-demo">
  
      <!-- 超时时间7秒 -->
      <joyrpc:method name="sayHello" timeout="7000"></joyrpc:method>
      
    </joyrpc:consumer>
  </beans>
  ```
