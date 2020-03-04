隐式传参
==
通过 RequestContext 在服务消费方和提供方之间进行参数的隐式传递。
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
每次调用前都需要设置一下参数，调用完后会自动清理请求上下文。

```java
RequestContext context=RequestContext.getContext();
context.setAttachment("user", "joyrpc");
context.setAttachment(".password", "111222");// "."开头的对应上面的hide=true
demoService.sayHello("test");
//调用完就不能访问到"user"上下文了
```

如果想在多次调用的时候共享上下文，可以使用会话请求上下文，但需要人工释放。

```java
RequestContext context=RequestContext.getContext();
context.setSession("user", "joyrpc");
context.setSession(".password", "111222");// "."开头的对应上面的hide=true
try{
    demoService.sayHello("test");
    //调用完还能共享会话上下文
    demoService.sayHello("hello");
}finally{
    //手动清空会话上下文
    context.clearSession();
}

```

如果是异步调用想在应答的时候访问请求上下文，请提前保存上下文变量

```java
RequestContext context=RequestContext.getContext();
context.setAttachment("user", "joyrpc");
context.setAttachment(".password", "111222");// "."开头的对应上面的hide=true
demoService.sayHello("test").whenComplete((result,error)->{
    String user=context.getAttachment("user");
    //省略
});
```

- Server端

 ```java
RequestContext.getContext().getAttachment("user");
RequestContext.getContext().getAttachment(".password");// "."开头的对应上面的hide=true
 ```

 
