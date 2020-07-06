多注册中心
==
1. JOYRPC 支持同一服务向多个注册中心同时注册，同时支持注册中心自定义扩展。

2. 已默认提供ZK、ETCD、广播和内存注册中心等扩展，用户简单设置参数即可连接使用。

3. 同时为方便用户使用，提供自定义memory内存注册中心，使用户无需启动第三方注册中心即可使用。

4. 有多个注册中心时，consumer 需指定一个注册中心调用；只有一个注册中心时，无需指定。

 >说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。
 
### Spring xml 方式

   ```xml
     <beans>
      <!-- 注册中心A -->
      <joyrpc:registry id="zk1" address="192.168.1.100:2181" registry="zk"/>
       
      <!-- 注册中心B -->
      <joyrpc:registry id="zk2" address="192.168.1.101:2181" registry="zk"/>
       
      <joyrpc:server id="myJoy"/>
       
      <!-- 发布到指定注册中心，多个注册中心用逗号分隔 -->
      <joyrpc:provider  id="demoProvider" interface="io.joyrpc.service.demoService" alias="joyrpc-demo" ref="demoServiceImpl" server="myJoy" registry="zk1,zk2"></joyrpc:provider>
      
      <!-- 调用者配置  -->
      <joyrpc:consumer id="demoConsumer" interface="io.joyrpc.service.demoService" alias="joyrpc-demo" registry="zk1"></joyrpc:consumer>
     </beans>
   ```
      
>registry 不指定时，默认发布到所有注册中心

### API 方式

  - 编写服务端代码
  
      ```java
        public class RegistryServerAPI {
        
            public static void main(String[] args) throws Exception {
                DemoService demoService = new DemoServiceImpl();
                /**
                 * 服务发布到A、B两个注册中心
                 */
                RegistryConfig zk1 = new RegistryConfig("zk", "192.168.1.100:2181");// 注册中心A
                RegistryConfig zk2 = new RegistryConfig("zk", "192.168.1.101:2181");// 注册中心B
                List<RegistryConfig> list = new ArrayList<>();
                list.add(zk1);
                list.add(zk2);
                ProviderConfig<DemoService> providerConfig = new ProviderConfig<DemoService>();
                providerConfig.setRegistry(list);
        
                providerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
                providerConfig.setRef(demoService);
                providerConfig.setAlias("joyrpc-demo");
        
                providerConfig.export().whenComplete((v, t) -> {//发布服务
                    providerConfig.open();
                });
                System.in.read();
            }
        }
      ```
      
  - 编写客户端代码
     
       ```java
         public class RegistryClientAPI {
         
             public static void main(String[] args) throws Exception {
                 DemoService demoService = new DemoServiceImpl();
                 /**
                  * 服务发布到A、B两个注册中心
                  */
                 RegistryConfig zk1 = new RegistryConfig("zk", "192.168.1.100:2181");// 注册中心A
                 RegistryConfig zk2 = new RegistryConfig("zk", "192.168.1.101:2181");// 注册中心B
                 List<RegistryConfig> list = new ArrayList<>();
                 list.add(zk1);
                 list.add(zk2);
                 ProviderConfig<DemoService> providerConfig = new ProviderConfig<DemoService>();
                 providerConfig.setRegistry(list);
         
                 providerConfig.setInterfaceClazz("io.joyrpc.service.DemoService");
                 providerConfig.setRef(demoService);
                 providerConfig.setAlias("joyrpc-demo");
         
                 providerConfig.export().whenComplete((v, t) -> {//发布服务
                     providerConfig.open();
                 });
                 System.in.read();
             }
         }
       ```
       
- 各种类型注册中心配置

  |名称|插件名称(registry)|默认端口|描述|
  | :----: | :----: | :----: | :----: |
  | ZooKeeper| zk | 2181 ||
  | ETCD| etcd | 2379 ||
  | 广播模式| broadcast | 5700 |组播|
  | 内存注册中心| memory | ||