隐式传参
==
通过 RequestContext 在服务消费方和提供方之间进行参数的隐式传递。
默认情况下，设置的参数只作用于一次调用，调用完后自动清理掉。
我们先看看隐式传参的场景。

- **场景1**：A调用B的时候，期望给B传递一下额外参数，只让B服务感知到。
- **场景2**：A同时调用B和C服务，期望给B和C都传递一下同样的额外参数，让B和C服务都感知到。
- **场景3**：A调用B服务，B服务顺序调用D和E服务。期望给B、D和E调用链都传递同样的额外参数

>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

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

这个配置能应用于场景1和场景2的静态参数

### API方式

API方式能应用于场景1、场景2和场景3，不管静态还是动态配置

#### 消费端API调用

- **场景1**

每次调用前都需要设置一下参数，调用完后会自动清理请求上下文。

```java
RequestContext context=RequestContext.getContext();
context.setAttachment("user", "joyrpc");
context.setAttachment(".password", "111222");// "."开头的对应上面的hide=true
demoService.sayHello("test");
//调用完就不能访问到"user"上下文了
```

- **场景2**

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

- **场景3**

传递调用链参数

```java
RequestContext context=RequestContext.getContext();
context.setTrace("orderId", "11111111");
demoService.sayHello("test");
```

#### 服务端API调用

服务端获取调用方传递的参数

 ```java
RequestContext context=RequestContext.getContext();
context.getAttachment("user");
context.getAttachment(".password");
 ```

 
