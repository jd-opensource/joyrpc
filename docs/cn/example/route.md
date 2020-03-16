集群分发策略
==

>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

## 配置
消费者调用时，可以设置集群分发策略，配置如下：

  ````xml
    <beans>
    
        <joyrpc:consumer cluster="failfast" />
    
    </beans>
  ````
### 集群分发策略

目前支持如下几种集群策略：

  |名称|配置|说明|
  | :----: | :----: | :---- |
  | 失败忽略 | failfast | 只发起一次调用，失败立即报错。约等于不重试的failover |
  | 失败重试（默认） | failover | 1.非业务失败则自动切换到下一台重新发起调用，增强用户体验。<br/>2.重试可能会导致调用时间更长（例如2s超时，重试2次，耗时最长可能会是6秒） 3.重试次数目前默认不重试，可以通过配置设置。 <[joyrpc:consumer] **retries**="2" /> 表示整个接口下所有方法都失败后重试2次。 <[joyrpc:method] method="hello" **retries**="1" /> 表示hello这个方法失败后重试1次。 |
  | 定点调用 | pinpoint | 按照用户指定的地址进行路由，目标地址可以由RequestContext中的".pinpoint"参数指定 |

### 重试策略扩展配置

可以配置在接口上也可以配置在方法上

  |名称|默认值|说明|
  | :----: | :----: | :---- |
  | retries |  | 最大重试次数 |
  | retryOnlyOncePerNode |  | 每个节点是否只调用一次 |
  | failoverWhenThrowable |  | 可以重试的异常全路径类名，多个异常类名用逗号分隔 |
  | failoverPredication |  | 自定义异常重试判断扩展点名称 |
  | failoverSelector | simple | 异常重试目标节点选择器 |
  
### 重试超时控制

重试的超时时间受整个的调用超时控制，当超过了调用超时会自动结束重试

超时时间可以配置在接口上也可以配置在方法上

  |名称|默认值|说明|
  | :----: | :----: | :---- |
  | timeout |  | 调用超时时间 |