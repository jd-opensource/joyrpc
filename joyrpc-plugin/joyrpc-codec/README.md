#序列化和编解码

- json只保留了fastjson
    - dsl-json功能弱，如果transient字段有getter、setter方法，会序列化该字段，并且异常不能正常序列化
    - jackson功能弱，对父类字段不支持，如Object字段
- hessian-lite有问题，写入的最后的bytes不能正常读取。和原生的hessian2比较代码，不同，缺少很多判断。com.alibaba.com.caucho.hessian.io.Hessian2Input.readBytes(byte[], int, int)