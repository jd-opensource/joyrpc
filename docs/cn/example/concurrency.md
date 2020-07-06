并发控制
==
Provider和Consumer都支持方法级别的并发控制
通过concurrency属性进行配置，方法级的配置可覆盖接口级的配置，接口级没配置则走默认。
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

配置-1关闭并发过滤器，等于0表示开启过滤但是不限制

### Provider端
>主要控制的是业务线程池的使用数量

  例如：一个Server业务线程池默认最大200，下面有2个接口，每个接口有5个方法，那么这10个方法的请求是共享这200个业务线程池的。
  为了防止一个方法（例如出异常的时候）占用了全部线程池，则要求每个方法最多使用50个线程（不一定要均分，不是200/10=20这样子，也可以超过20）。

  Provider端如果来请求时，超过并发大小，调用就会立即抛出RPC异常给客户端：

  配置如下：
  ```xml
  <beans>
    <!-- 默认这个server最大200个线程 -->
    <joyrpc:server threads="200" /> 
    
    <!-- 接口下每个方法控制在50并发，即最大50个业务线程同时执行-->
    <joyrpc:provider concurrency="50"> 
    
        <!-- 这个方法比较特别，控制在10并发，最大10个业务线程同时执行-->
        <joyrpc:method name="getHashSet" concurrency="10" /> 
        
    </joyrpc:provider>
  
  </beans>
  
  ```

### Consumer端
>主要控制调用者自己的业务线程同时发起的请求数。

  Consumer如果来请求时，超过并发大小，则会等待执行直到超时为止。

  ```xml
  <beans>
  
    <!-- 接口下每个方法控制在50并发，即最大50个业务线程在调用-->
    <joyrpc:consumer concurrency="50"> 
    
        <!-- 控制在10并发，最大10个业务线程在调用-->
        <joyrpc:method name="getHashSet" concurrency="10" /> 
        
    </joyrpc:consumer>
  </beans>
  ```