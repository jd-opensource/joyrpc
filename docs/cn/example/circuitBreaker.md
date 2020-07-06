熔断限流
===

## 熔断

### 熔断配置

熔断只是在发生异常的时候触发，其参数配置如下

| 参数 | 名称 | 类型 | 默认值 | 说明 |
| :--- | :---- | :---- |:---- |:---- |
| circuitBreaker.enable | 启用标识 |  boolean | false |   |
| circuitBreaker.period | 熔断时间 | long | 10000 | 单位毫秒 |
| circuitBreaker.decubation | 恢复时间 | long | 10000 | 熔断结束后，逐渐恢复到正常权重的时间 |
| circuitBreaker.successiveFailures | 触发熔断的连续失败次数阈值 | int | 0 |  |
| circuitBreaker.availability | 触发熔断的可用率阈值 | double | 0 |  |
| circuitBreaker.exception | 触发熔断的异常类 | String | io.joyrpc.exception.OverloadException<br/>java.util.concurrent.TimeoutException | 类的全路径名，多个以逗号隔开 |

可以配置在全局参数里面，对所有消费者生效，或者也可以对指定消费者及其方法进行配置。

方法配置优先级>消费者配置>全局参数配置

配置样例如下

  ```xml
  <beans>
    <joyrpc:parameter key="circuitBreaker.enable" value="true" />
    <joyrpc:parameter key="circuitBreaker.availability" value="90" />
    <joyrpc:consumer  interface="io.joyrpc.service.DemoService" alias="joyrpc-demo">
        <joyrpc:parameter key="circuitBreaker.availability" value="95" />
        <joyrpc:method name="sayHello">
            <joyrpc:parameter key="circuitBreaker.availability" value="97" />
        </joyrpc:method>
    <joyrpc:consumer/>
  </beans>
  ```
