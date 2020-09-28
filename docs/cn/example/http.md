HTTP调用
==
服务端支持标准的http调用和json-rpc协议。
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

## 1. 标准HTTP调用

### 1.1 支持的HTTP方法

POST/PUT

### 1.2 URL格式：

- http://ip:port/interface/alias/method
- http://ip:port/interface/method

  alias通过http header传递

### 1.3 BODY参数

Body为JSON格式，可以采用如下两种格式
- JSON数组，例如：[1,2]
- JSON对象，以参数名作为key，例如：{"id":1,"value":{"user":"aaa"}}

## 2. JSON-RPC调用

### 2.1 支持的HTTP方法

POST/PUT

### 2.2 URL格式：

可以采用如下两种格式
- http://ip:port/interface

  alias通过header传递

- http://ip:port/interface/alias

### 2.3 BODY参数

仅支持单条json-rpc调用，不支持批量调用

## 3. Header传参

提供了HeaderInjection扩展点，系统内置了默认实现，支持如下传参

1. header中以"."开头的隐式参数
2. header中以"X-HIDDEN-"开头的隐式参数
3. header中以"X-TRANS-"开头的普通参数