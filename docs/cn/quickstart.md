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
#### 2.1.2 编写服务端实现

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
      
              ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>(); //consumer设置
              consumerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
              consumerConfig.setAlias("joyrpc-demo");
              consumerConfig.setRegistry(registryConfig);
              try {
                  CompletableFuture<Void> future = new CompletableFuture<Void>();
                  DemoService service = consumerConfig.refer(future);
                  future.get();
                  String echo = service.sayHello("hello"); //发起服务调用
      
                  System.in.read();
              } catch (Exception e) {
              }
          }
      }
      ```

### 2.2 Spring方式

### 2.3 SpringBoot方式 