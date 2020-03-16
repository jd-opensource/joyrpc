调用压缩
==
在Consumer发送请求和Provider返回响应的时候，都可以开启调用压缩。
目前支持多种算法：lz4(默认)、snappy、lzma、zlib
>说明：下面示例中采用  **`<joyrpc/>`** 标签 表示JOYRPC中的schema。

### consumer设置

如果Consumer端配置了压缩，且请求的数据大于2048B，那么请求数据将被压缩后再发给Provider。

  ```xml
  <beans>
    <joyrpc:consumer compress="lz4" />
  </beans>
  ```
### provider设置

如果Provider端配置了压缩，那么不管请求时是不是带压缩标识，返回响应的时候也都会检查数据大小，如果数据大小超过阈值，则将响应数据压缩后再发给Consumer。

  ```xml
  <beans>
    <joyrpc:provider compress="lz4" />
  </beans>
  ```

**最佳实践**：请求数据大的客户端配置，响应数据大的服务端配置。