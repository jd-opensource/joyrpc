延迟启动
==
服务发布的时候，spring在加载到 joyrpc:provider的时候默认会马上发布服务。
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

如果需要延迟一段时间再发布服务，可以通过如下配置实现：

  ```xml
  <beans>
  
     <!-- 配置10000,表示延迟十秒发布;配置为-1,表示即时发布-->
    <joyrpc:provider delay="10000" />
    
  </beans>
  ```
