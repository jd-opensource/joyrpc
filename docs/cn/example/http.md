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

  | Header参数 | 参数值 | 说明 |
  | :---- | :---- | :---- |
  | alias | | 分组别名 |

### 1.3 BODY参数

Body为JSON格式，可以采用如下两种格式
- JSON数组，例如：[1,2]
- JSON对象，以参数名作为key，例如：{"id":1,"value":{"user":"aaa"}}

## 2. JSON-RPC调用

### 2.1 支持的HTTP方法

POST/PUT

### 2.2 URL格式：

可以采用如下四种格式
- http://ip:port/interface
  
  | Header参数 | 参数值 | 说明 |
  | :---- | :---- | :---- |
  | alias | | 分组别名 |
  | Content-Type| application/json-rpc | |

- http://ip:port/interface/alias
  
  | Header参数 | 参数值 | 说明 |
  | :---- | :---- | :---- |
  | Content-Type| application/json-rpc | |

- http://ip:port/jsonrpc/interface/alias

- http://ip:port/jsonrpc/interface

  | Header参数 | 参数值 | 说明 |
  | :---- | :---- | :---- |
  | alias | | 分组别名 |

### 2.3 BODY参数

仅支持单条json-rpc调用，不支持批量调用，如下：

```json
{"jsonrpc": "2.0", "method": "sum", "params": [1,2,4], "id": "1"}
```

## 3. Header传参

提供了HeaderInjection扩展点，系统内置了默认实现，支持如下传参

| 参数 | 说明 |
| :---- | :---- | 
| "."开头 | 隐式参数 |
| "X-HIDDEN-"开头 | 隐式参数 |
| "X-TRANS-"开头 | 普通参数 |