快速开始
===

## 1. 依赖

  需要安装 JDK 8 及以上 和 Maven 3 以上

   ```xml
   <dependency>
     <groupId>io.joyrpc</groupId>
     <artifactId>joyrpc-all</artifactId>
     <version>最新版本</version>
   </dependency>
   ```
## 2. 演示例子

### 2.1 API方式

#### 2.1.1 编写服务端实现

  - 创建接口
  
      ```java
      /**
       * Demo interface
       */
      public interface DemoService {
        String sayHello(String str);
      }
      ```
      
  - 创建接口实现
  
       ```java
       public class DemoServiceImpl implements DemoService {
           public String sayHello(String name) {
             return "Hi " + name + ", response from provider. ";
           }
       }
       ```
  - 编写服务端代码
  
      ```java
      public class ServerAPI {
      
          public static void main(String[] args) throws Exception {
              RegistryConfig registryConfig = new RegistryConfig();
              registryConfig.setRegistry("memory");//内存注册中心
      
              DemoService demoService = new DemoServiceImpl(); //服务提供者设置
              ProviderConfig<DemoService> providerConfig = new ProviderConfig<>();
              providerConfig.setServerConfig(new ServerConfig());
      
              providerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
              providerConfig.setRef(demoService);
              providerConfig.setAlias("joyrpc-demo");
              providerConfig.setRegistry(registryConfig);
      
              providerConfig.export().whenComplete((v, t) -> {//发布服务
                  providerConfig.open();
              });
              System.in.read();
          }
      }
      ```
#### 2.1.2 编写客户端实现

  - 拿到服务端接口
  
       通常以jar的形式将接口类提供给客户端。在此，先定义全路径相同的接口做演示。
      
       ```java
        /**
         * Demo interface
         */
        public interface DemoService {
           String sayHello(String str);
        }
        
       ```
       
  - 编写客户端代码
  
      ```java
      public class ClientAPI {
      
          public static void main(String[] args) {
              RegistryConfig registryConfig = new RegistryConfig();
              registryConfig.setRegistry("memory");//内存注册中心
      
              ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>();//consumer设置
              consumerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
              consumerConfig.setAlias("joyrpc-demo");
              consumerConfig.setRegistry(registryConfig);
              try {
                  CompletableFuture<Void> future = new CompletableFuture<Void>();
                  DemoService service = consumerConfig.refer(future);
                  future.get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
                  String echo = service.sayHello("hello");//发起服务调用
      
                  System.in.read();
              } catch (Exception e) {
              }
          }
      }
      ```

### 2.2 Spring方式
   
  > spring配置文件中需引入XSD文件

  ```xml
  <?xml version="1.0" encoding="UTF-8"?>
  <beans xmlns="http://www.springframework.org/schema/beans"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns:joyrpc="http://joyrpc.io/schema/joyrpc"
         xsi:schemaLocation=
         "http://www.springframework.org/schema/beans  http://www.springframework.org/schema/beans/spring-beans.xsd
         http://joyrpc.io/schema/joyrpc  
         http://joyrpc.io/schema/joyrpc/joyrpc.xsd">
  </beans>
  ```
   >说明：上面是完整的schema描述，下面示例中采用  **`<joyrpc/>`** 标签 代表此schema。

#### 2.2.1 编写服务端实现

   - 编写服务端代码
   
      ```java
      public class ServerMain {
          private static final Logger LOGGER = LoggerFactory.getLogger(ClientMain.class);
      
          public static void main(String[] args) throws Exception {
              ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("/joyrpc-provider.xml");
      
              LOGGER.info("服务端启动完成！");
              System.in.read();
          }
      }
      ```

   
   - 编写服务端配置

      resources目录下定义配置文件：joyrpc-provider.xml
  
       ```xml
        <beans>
   
          <!-- 实现类 -->
          <bean id="demoServiceImpl" class="io.joyrpc.service.impl.DemoServiceImpl"/>
      
          <!-- 注册中心 -->
          <joyrpc:registry id="joyRpcRegistry" address="memory://127.0.0.1" registry="memory"/>
      
          <!-- 服务端 端口默认22000 -->
          <joyrpc:server id="joyRpcServer"/>
      
          <!-- 发布服务 alias可修改 -->
          <joyrpc:provider id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" ref="demoServiceImpl" server="joyRpcServer"></joyrpc:provider>
     
      </beans>
      ```

#### 2.1.2 编写客户端实现

   - 编写客户端代码

        ```java
            public class ClientMain {
                private static final Logger LOGGER = LoggerFactory.getLogger(ClientMain.class);
            
                public static void main(String[] args) throws Exception {
                    ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("/joyrpc-consumer.xml");
                    DemoService service = (DemoService) appContext.getBean("demoService");
            
                    try {
                        String result = service.sayHello("hello");
                        LOGGER.info("response msg from server :{}", result);
                    } catch (Exception e) {
                    }
               
                    System.in.read();//终端输入任意字符，shutdown进程
                }
            }
        ```
   - 编写客户端配置

        resources目录下定义配置文件：joyrpc-consumer.xml
        
        ```xml
           <beans>
      
             <!-- 注册中心 -->
             <joyrpc:registry id="joyprpcRegistry" address="memory://127.0.0.1" registry="memory"/>
         
             <!-- 调用者配置 -->
             <joyrpc:consumer id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo"></joyrpc:consumer>
           </beans>
        ```

### 2.3 SpringBoot方式 

   待补充

### 运行

   分别启动服务端和客户端，观察运行效果。

   服务端将打印：

   >Hi hello, request from consumer.

   客户端将打印：

   >Hi hello, response from provider

### 更多

   更多示例请参考：[example](../../joyrpc-example)