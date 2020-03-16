直连调用
==
为方便开发及测试，通常可以采用直连方式完成RPC调用，忽略注册中心，但是不建议线上使用，因为无法通过注册中心拿到新的服务列表地址，可扩展性差。

具体是由Consumer调用的时候直接指定Provider地址进行远程调用。

>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

### Spring xml配置

   ````xml
     <beans>
       <!-- 格式: 协议://ip:port/?key=value, 多个用英文逗号或英文分号隔开-->
       <joyrpc:consumer url="joyrpc://192.168.1.100:22000,joyrpc://192.168.1.101:22001" ></joyrpc:consumer>
     </beans>
   ````
   
### API方式

   ```java
     public class RegistryClientAPI { 
    
       public static void main(String[] args) throws Exception {
          ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>();
          consumerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
          consumerConfig.setAlias("joyrpc-demo");
          consumerConfig.setSubscribe(false);//关闭订阅
          consumerConfig.setRegister(false);//关闭注册
          
          consumerConfig.setUrl("joyrpc://" + Ipv4.getLocalIp() + ":" + 22000);//优先直连,本地/默认22000端口
         
          try {
              CompletableFuture<Void> future = new CompletableFuture<Void>();
              DemoService service = consumerConfig.refer(future);
              future.get();
         
              String echo = service.sayHello("hello"); //发起服务调用
              LOGGER.info("response msg from server :{}", echo);
         
              System.in.read();
          } catch (Exception e) {
          }
       }
     }
   ```

