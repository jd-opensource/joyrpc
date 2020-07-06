预热接口
==
预热：服务启动完毕后，通过插件实现自定义业务逻辑
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

### 1.写一个warmup实现类，定义Extension

  Provider侧实现Warmup接口，重写setup方法
  
   ```java
  @Extension("warmlog")
  public class WarmupLog  implements Warmup {
      private static final Logger logger = LoggerFactory.getLogger(WarmupLog.class);
   
      @Override
      public void setup(AbstractInterfaceConfig config) {
          logger.info("i am warmup");
      }
  }
   ```
### 2.定义配置文件

  - 在Maven工程中java/main/resources路径下的META-INF/services/目录中创建以接口全限定名（包名+接口名）命名的文件。（没有META-INF/services目录时，需要新建）

  - 文件名为：接口的全限定名约定：io.rpc.config.Warmup  备注：严格按约定名称写

  - 文件内容：写入实现的全限定实现类（包名+类名）io.rpc.config.warmup..WarmupLog   备注：用户实现类所在的包名+类名


### 3.provider中设置

  ```xml
  <beans>
  
    <!-- 发布服务 -->
    <joyrpc:provider id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo" ref="demoServiceImpl" server="myJoy" warmup="warmlog"> <!-- 配置预热插件 -->
    </joyrpc:provider>
  </beans>
  ```