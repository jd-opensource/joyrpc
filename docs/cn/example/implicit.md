隐式传参
==
通过 RequestContext 上的 setAttachment 和 getAttachment 在服务消费方和提供方之间进行参数的隐式传递。
>说明：下面示例中采用  **`<beans/>`** 标签 表示JOYRPC中的schema。

### Spring配置方式

```xml
<beans>
<joyrpc:consumer id="demoService" interface="io.joyrpc.service.DemoService" alias="joyrpc-demo">

     <!-- 接口级参数，标记为hide -->
     <joyrpc:parameter key="passwd" value="11112222" hide="true" />
     
     <joyrpc:method name="echo">
     
        <!-- 方法级参数，标记为hide -->
        <joyrpc:parameter key="bbb" value="bbbbbbb" hide="true"/>  
     </joyrpc:method>
</joyrpc:consumer>
</beans>
```

### API方式
- Consumer端
```
RequestContext.getContext().setAttachment("user", "jsf2.0");
RequestContext.getContext().setAttachment(".password", "111222");// "."开头的对应上面的hide=true
```
- Server端
 ```
RequestContext.getContext().getAttachment("user");
RequestContext.getContext().getAttachment(".password");// "."开头的对应上面的hide=true
 ```

 
