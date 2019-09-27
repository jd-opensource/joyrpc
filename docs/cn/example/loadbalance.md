负载均衡
==
在Consumer调用，会对服务列表进行软负载，配置如下：

  ````xml
    <beans>
      <joyrpc:consumer loadbalance="randomWeight" />
    </beans>
  ````
目前支持如下几种策略:

  |名称|配置|说明|
  | :----: | :---- | :---- |
  | 加权随机负载均衡（默认) | randomWeight | 1.按provider端配置的权重的进行随机，可以方便的调整  <br/>2.调用量越大分布越均匀 |
  | 自适应 | adaptive | 按TP值及可用率（异常会影响可用率）做负载判断优先选择TP值高且可用率高的连接（先TP后可用率） |
