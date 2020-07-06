代理类配置
==
支持bytebuddy（默认）、javassist、jdk 三种代理类生成方式。
主要作用就是在调用端拦截下业务代码的本地调用，转为调用远程服务端。可通过配置进行设置。
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。


  ```xml
  <beans>
  
  <joyrpc:provider proxy="bytebuddy" />
  
  <joyrpc:consumer proxy="javassist" />
  
  </beans>
  ```