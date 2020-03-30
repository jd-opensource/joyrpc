package io.joyrpc.codec.serialization.protostuff;

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

import io.joyrpc.codec.serialization.ObjectWriter;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.Serializer;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufOutput;
import io.protostuff.ProtostuffWriter;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Protostuff
 */
@Extension(value = "protobuf", provider = "protostuff", order = Serialization.ORDER_PROTOBUF)
@ConditionalOnClass("io.protostuff.runtime.RuntimeSchema")
public class ProtobufSerialization implements Serialization {

    @Override
    public byte getTypeId() {
        return PROTOBUF_ID;
    }

    @Override
    public String getContentType() {
        return "application/x-protobuf";
    }

    @Override
    public Serializer getSerializer() {
        return ProtobufSerializer.INSTANCE;
    }

    /**
     * Protostuff序列化和反序列化实现
     */
    protected static final class ProtobufSerializer extends ProtostuffSerialization.ProtostuffSerializer {

        protected static final ProtobufSerializer INSTANCE = new ProtobufSerializer();

        @Override
        protected ObjectWriter createWriter(final OutputStream os, final Object object) throws IOException {
            LinkedBuffer buffer = local.get();
            return new ProtostuffWriter(RuntimeSchema.getSchema(object.getClass(), STRATEGY), buffer, new ProtobufOutput(buffer), os);
        }

    }

}
