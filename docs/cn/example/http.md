HTTP调用
==
服务端支持标准的http调用和json-rpc协议。
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

## 标准HTTP调用

### 支持的HTTP方法

POST/PUT

### URL格式：

1. http://ip:port/interface/alias/method
2. http://ip:port/interface/method，alias通过header传递

### BODY参数

Body支持JSON格式
1. 数组格式，例如：[1,2]
2. Map格式，以参数名作为key，例如：{"id":1,"value":{"user":"aaa"}}

## JSON-RPC调用

### 支持的HTTP方法

POST/PUT

### URL格式：

1. http://ip:port/interface/，alias通过header传递
2. http://ip:port/interface/alias

### BODY参数

支持单条json-rpc调用

## Header传参

提供了HeaderInjection扩展点，系统内置了默认实现，支持如下传参

1. header中以"."开头的隐式参数
2. header中以"X-HIDDEN-"开头的隐式参数
3. header中以"X-TRANS-"开头的普通参数