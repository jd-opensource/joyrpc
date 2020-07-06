异步调用
==
JOYRPC 引入了JDK8 中的 CompletableFuture 类，实现了真正意义上的异步调用。
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

- 适用场景：
可以一次性并行发起多个请求服务的环境（例如某些集成页面，后台同时调用多个系统），总耗时等于最长时间的调用； 不适合服务间有依赖的情况。
例如：

```
一个页面要同时调用A服务(耗时600ms)，B服务(耗时500ms)，C服务(耗时100ms) 三个服务。
如果采取线性调用，则需要1200ms；采取异步调用则只需要600ms。
```
- 使用方式

 方法返回值定义为CompletableFuture

### 1. 发布含异步返回值的方法

```java
public interface DemoService {
   /**
     * 异步调用,按返回类型自动判定为异步：CompletableFuture
     *
     * @param str
     * @param microsecond ms
     * @return CompletableFutur
     */
    CompletableFuture<String> sayHelloAsync(String str, long microsecond);
}
```

### 含异步返回值方法实现

```java
public class DemoServiceImpl implements DemoService {
    private static final Logger logger = LoggerFactory.getLogger(DemoServiceImpl.class);
 
    @Override
    public CompletableFuture<String> sayHelloAsync(String str, long microsecond) {
        try {
            Thread.sleep(microsecond);
        } catch (InterruptedException e) {
            logger.error("error: {}", e);
        }
        return CompletableFuture.completedFuture(str);
    }
}
```
### consumer异步调用

```java
public class SyncClient1MainAPI {
    private static final Logger logger = LoggerFactory.getLogger(SyncClient1MainAPI.class);
 
    public static void main(String[] args) throws Exception {
        ConsumerConfig<DemoService> consumerConfig = new ConsumerConfig<>();
        consumerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
        consumerConfig.setAlias("2.0-Demo");
        consumerConfig.setTimeout(30 * 1000);//30s
 
        CompletableFuture<Void> future = new CompletableFuture<>();
        DemoService service = consumerConfig.refer(future);
        future.get();
        /**
         * 场景，发起三次调用，每次调用时间如下：
         * f1: 3s
         * f2: 3s
         * f3: 10s
         *
         * 1. 同步调用耗时：3+3+10=16s 左右
         * 2. 异步调用耗时  max(3,3,10)= 10s 左右
         *
         */
        long beginTime = System.currentTimeMillis();
        try {
            CompletableFuture<String> f1 = service.sayHelloAsync("first", 3000);
            CompletableFuture<String> f2 = service.sayHelloAsync("second", 3000);
            CompletableFuture<String> f3 = service.sayHelloAsync("third", 10000);
            String r1 = f1.get();
            String r2 = f2.get();
            String r3 = f3.get();
            logger.info("Get first Response : {}", r1);
            logger.info("Get second Response : {}", r2);
            logger.info("Get third Response : {}", r3);
 
        } catch (Exception e) {
            logger.info("Get exception : {}", e);
        } finally {
            logger.info("cost {} ms", System.currentTimeMillis() - beginTime);
        }
        //终端输入任意字符，shutdown进程
        System.in.read();
    }
}
```
