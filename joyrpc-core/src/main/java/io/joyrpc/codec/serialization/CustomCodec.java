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

import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.Type;

import java.io.IOException;

/**
 * 自定义编码器实现类
 *
 * @param <T>
 */
@Extensible("customCodec")
public interface CustomCodec<T> extends Type<Class> {

    /**
     * 序列化
     *
     * @param output
     * @param object
     * @throws IOException
     */
    void encode(ObjectWriter output, T object) throws IOException;

    /**
     * 反序列化
     *
     * @param input
     * @throws IOException
     */
    T decode(ObjectReader input) throws IOException;

}
