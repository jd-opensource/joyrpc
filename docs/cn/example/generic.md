泛化调用
==
当调用者是拿不到服务端的class或者jar包，那此时发起调用，改如何处理？

JOYRPC支持泛化调用，只需要指定接口、方法、参数类型、参数值，就可以完成调用。

>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。


### 泛化接口

  ```java
  /**
   * 泛化调用接口，由于没有目标类，复杂参数对象采用Map进行传输（原理和JsonObject类似)
   * 泛型调用不会出现Callback
   */
  public interface GenericService {
      /**
           * 同步泛化调用，兼容
           *
           * @param method         方法名
           * @param parameterTypes 参数类型
           * @param args           参数列表
           * @return 返回值
           */
          default Object $invoke(final String method, final String[] parameterTypes, final Object[] args) {
              try {
                  return $async(method, parameterTypes, args).get(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
              } catch (InterruptedException e) {
                  throw new RpcException(String.format("Failed invoking %s, It's interrupted.", method), e);
              } catch (ExecutionException e) {
                  Throwable throwable = e.getCause() == null ? e : e.getCause();
                  if (throwable instanceof RpcException) {
                      throw (RpcException) throwable;
                  }
                  throw new RpcException(String.format("Failed invoking %s, caused by %s", method, throwable.getMessage()), throwable);
              } catch (TimeoutException e) {
                  throw new RpcException(String.format("Failed invoking %s, It's timeout.", method), e);
              }
          }
      
          /**
           * 异步泛化调用
           *
           * @param method         方法名
           * @param parameterTypes 参数类型
           * @param args           参数列表
           * @return CompletableFuture
           */
          CompletableFuture<Object> $async(String method, String[] parameterTypes, Object[] args);
  
  }
  ```

### Consumer调用

  需要配置`generic="true"`
  
  ```java
  public class GenericClientMainAPI {
  
      private static final Logger logger = LoggerFactory.getLogger(GenericClientMainAPI.class);
  
      public static void main(String[] args) throws Exception {
          ConsumerConfig<GenericService> consumerConfig = new ConsumerConfig<>();
          consumerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
          consumerConfig.setAlias("joyrpc-Demo");
  
          //开启泛化调用
          consumerConfig.setGeneric(true);
  
          CompletableFuture<Void> future = new CompletableFuture<>();
          GenericService service = consumerConfig.refer(future);
          future.get();
  
          try {
              CompletableFuture<Object> sayHelloFuture = service.$async("sayHello", new String[]{String.class.getName()}, new Object[]{"GENERIC"});
              Object res = sayHelloFuture.get();
              logger.info("Get Response : {}", res);
          } catch (Exception e) {
              logger.info("Get exception : {}", e);
          }
  
          //终端输入任意字符，shutdown进程
          System.in.read();
      }
  }
  
  ```
  

