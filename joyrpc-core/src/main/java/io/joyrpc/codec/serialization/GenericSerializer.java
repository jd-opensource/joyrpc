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

import io.joyrpc.exception.CodecException;
import io.joyrpc.extension.Extensible;
import io.joyrpc.protocol.message.Call;

/**
 * 泛化调用序列化处理。对请求参数和调用结果进行处理。便于网关透传调用
 */
@Extensible("genericSerializer")
public interface GenericSerializer {

    /**
     * 泛化参数数据序列化插件配置键名称
     */
    String GENERIC_SERIALIZER = ".genericSerializer";
    /**
     * 参数以JSON格式进行传输
     */
    String JSON = "json";

    /**
     * 标准泛化参数处理
     */
    String STANDARD = "standard";

    /**
     * 结果序列化
     *
     * @param object 结果
     * @throws CodecException
     */
    Object serialize(Object object) throws CodecException;

    /**
     * 参数反序列化
     *
     * @param invocation 调用
     * @return
     * @throws CodecException
     */
    Object[] deserialize(Call invocation) throws CodecException;

}
