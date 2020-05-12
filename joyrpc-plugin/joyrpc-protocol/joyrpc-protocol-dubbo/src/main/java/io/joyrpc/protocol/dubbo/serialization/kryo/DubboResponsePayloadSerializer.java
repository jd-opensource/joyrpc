package io.joyrpc.protocol.dubbo.serialization.kryo;

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

import com.esotericsoftware.kryo.AutowiredObjectSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.joyrpc.codec.serialization.kryo.KryoReader;
import io.joyrpc.codec.serialization.kryo.KryoWriter;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.protocol.dubbo.message.DubboResponsePayload;

import java.io.IOException;

/**
 * DubboResponsePayload序列化
 */
@ConditionalOnClass("com.esotericsoftware.kryo.Kryo")
public class DubboResponsePayloadSerializer extends AutowiredObjectSerializer {

    @Override
    public Class<?> getType() {
        return DubboResponsePayload.class;
    }

    @Override
    public void write(final Kryo kryo, final Output output, final Object object) {
        try {
            ((DubboResponsePayload) object).write(new KryoWriter(kryo, output));
        } catch (IOException e) {
            throw new KryoException(e.getMessage(), e);
        }
    }

    @Override
    public Object read(final Kryo kryo, final Input input, final Class type) {
        try {
            return new DubboResponsePayload().read(new KryoReader(kryo, input));
        } catch (IOException e) {
            throw new KryoException(e.getMessage(), e);
        }
    }

}
