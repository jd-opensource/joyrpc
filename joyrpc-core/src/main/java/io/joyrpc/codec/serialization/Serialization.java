package io.joyrpc.codec.serialization;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.codec.CodecType;
import io.joyrpc.extension.Extensible;

/**
 * 对象序列化和反序列化提供者
 */
@Extensible("serialization")
public interface Serialization extends CodecType {

    int HESSIAN_ID = 2;

    int JAVA_ID = 3;

    /**
     * 兼容老版本的JSON输出格式
     */
    @Deprecated
    int JSON0_ID = 5;

    int KRYO_ID = 8;

    /**
     * 兼容老版本的JSON输出格式
     */
    @Deprecated
    int MSGPACK_ID = 10;

    int PROTOBUF_ID = 12;

    /**
     * 新的JSON格式
     */
    int JSON_ID = 13;

    int PROTOSTUFF_ID = 14;

    @Deprecated
    int HESSIAN_LITE_ID = 15;

    int MESSAGEPACK_ID = 16;

    int FST_ID = 17;

    int XML_ID = 100;

    int ORDER_PROTOSTUFF = 100;

    int ORDER_HESSIAN_LITE = ORDER_PROTOSTUFF + 10;

    int ORDER_HESSIAN = ORDER_HESSIAN_LITE + 10;

    int ORDER_DSLJSON = ORDER_HESSIAN + 10;

    int ORDER_FASTJSON = ORDER_DSLJSON + 10;

    int ORDER_JACKSON = ORDER_FASTJSON + 10;

    int ORDER_JAVA = ORDER_JACKSON + 10;

    int ORDER_FST = ORDER_JAVA + 10;

    int ORDER_KRYO = ORDER_FST + 10;

    int ORDER_MESSAGEPACK = ORDER_KRYO + 10;

    int ORDER_PROTOBUF = ORDER_MESSAGEPACK + 10;

    int ORDER_JPROTOBUF = ORDER_PROTOBUF + 10;

    int ORDER_JAXB = ORDER_PROTOSTUFF + 300;

    /**
     * 获取内容格式
     *
     * @return
     */
    String getContentType();

    /**
     * 构建序列化器
     *
     * @return
     */
    Serializer getSerializer();

    /**
     * 是否自动识别类型信息
     *
     * @return 支持自动识别类型
     */
    default boolean autoType() {
        return true;
    }

}
