package io.joyrpc.protocol.dubbo.serialization.protostuff;

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

import io.joyrpc.codec.serialization.*;
import io.joyrpc.codec.serialization.protostuff.ProtostuffSerialization;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.protocol.dubbo.serialization.protostuff.delegate.SqlDateDelegate;
import io.joyrpc.protocol.dubbo.serialization.protostuff.delegate.TimeDelegate;
import io.joyrpc.protocol.dubbo.serialization.protostuff.delegate.TimestampDelegate;
import io.protostuff.AutowiredObjectSerializer;
import io.protostuff.LinkedBuffer;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Protostuff
 */
@Extension(value = "protostuff", provider = "dubbo", order = Serialization.ORDER_DUBBO_PROTOSTUFF)
@ConditionalOnClass("io.protostuff.runtime.RuntimeSchema")
public class DubboProtostuffSerialization extends ProtostuffSerialization {

    @Override
    public byte getTypeId() {
        return DUBBO_PROTOSTUFF_ID;
    }

    @Override
    public Serializer getSerializer() {
        return DubboProtostuffSerializer.INSTANCE;
    }

    /**
     * DubboProtostuff序列化和反序列化实现
     */
    protected static class DubboProtostuffSerializer extends AbstractSerializer {

        protected static final DubboProtostuffSerializer INSTANCE = new DubboProtostuffSerializer();

        protected static final DefaultIdStrategy STRATEGY = new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS |
                IdStrategy.ALLOW_NULL_ARRAY_ELEMENT);

        static {
            STRATEGY.registerDelegate(new TimeDelegate());
            STRATEGY.registerDelegate(new TimestampDelegate());
            STRATEGY.registerDelegate(new SqlDateDelegate());
            //ID_STRATEGY.ARRAY_SCHEMA
            //注册插件，便于第三方协议注册序列化实现
            register(AutowiredObjectSerializer.class, o -> STRATEGY.registerPojo(o.getType(), o));
        }

        protected ThreadLocal<LinkedBuffer> local = ThreadLocal.withInitial(() -> LinkedBuffer.allocate(1024));

        protected DubboProtostuffSerializer() {
        }

        @Override
        protected ObjectWriter createWriter(final OutputStream os, final Object object) throws IOException {
            return new DubboProtostuffWriter(RuntimeSchema.getSchema(object.getClass(), STRATEGY), os, local.get());
        }

        @Override
        protected ObjectReader createReader(final InputStream is, final Class clazz) throws IOException {
            return new DubboProtostuffReader(RuntimeSchema.getSchema(clazz, STRATEGY), is);
        }
    }

}
