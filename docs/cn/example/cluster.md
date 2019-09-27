集群策略
==
Consumer调用时，可以设置调用集群策略，配置如下：
  ````xml
    <beans>
    <jsf:consumer cluster="failfast" />
    </beans>
  ````
目前支持如下几种集群策略：

  |名称|配置|说明|
  | :----: | :----: | :---- |
  | 失败忽略（默认） | failfast | 只发起一次调用，失败立即报错。约等于不重试的failover |
  | 失败重试 | failover | 1.非业务失败则自动切换到下一台重新发起调用，增强用户体验。<br/>2.重试可能会导致调用时间更长（例如2s超时，重试2次，耗时最长可能会是6秒） 3.重试次数目前默认不重试，可以通过配置设置。 <[jsf:consumer](http://jsfconsumer/) **retries**="2" /> 表示整个接口下所有方法都失败后重试2次。 <[jsf:method](http://jsfmethod/) method="hello" **retries**="1" /> 表示hello这个方法失败后重试1次。 |
  | 自适应 | adaptive | 按TP值及可用率（异常会影响可用率）做负载判断优先选择TP值高且可用率高的连接（先TP后可用率） |
