负载均衡
==
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

在Consumer调用，会对服务列表进行软负载，配置如下：

  ````xml
    <beans>
      <joyrpc:consumer loadbalance="randomWeight" sticky=”false"/>
    </beans>
  ````

参数说明

| 参数 | 名称 | 类型 | 默认值 | 说明 |
| :----: | :---- | :---- |:---- |:---- |
| loadbalance | 负载均衡算法 |  String | randomWeight |  1.randomWeight:加权随机负载均衡，会根据服务节点权重进行负载 <br/> 2.adaptive:自适应负载均衡，会根据服务端承受能力来进行动态负载<br/>3.roundRobin:轮询算法
| sticky | 粘连算法 | Boolean | false | 配合指定的loadbalance算法，尽量保持同一个目标地址。<br/>目标地址下线或出了异常再进行切换 |