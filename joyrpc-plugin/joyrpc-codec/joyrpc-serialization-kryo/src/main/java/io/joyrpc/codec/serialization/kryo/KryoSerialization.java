package io.joyrpc.codec.serialization.kryo;

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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.joyrpc.codec.serialization.*;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * kryo
 */
@Extension(value = "kryo", provider = "esotericsoftware", order = Serialization.ORDER_KRYO)
@ConditionalOnClass("com.esotericsoftware.kryo.Kryo")
public class KryoSerialization implements Serialization {

    @Override
    public byte getTypeId() {
        return KRYO_ID;
    }

    @Override
    public String getContentType() {
        return "application/x-kryo";
    }

    @Override
    public Serializer getSerializer() {
        return KryoSerializer.INSTANCE;
    }

    /**
     * Kryo序列化和反序列化实现
     */
    protected static final class KryoSerializer extends AbstractSerializer {

        protected static final ThreadLocal<Kryo> local = ThreadLocal.withInitial(() -> {
            final Kryo kryo = new Kryo();
            kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
            return kryo;
        });

        protected static final KryoSerializer INSTANCE = new KryoSerializer();


        protected KryoSerializer() {
        }

        @Override
        protected ObjectWriter createWriter(final OutputStream os, final Object object) throws IOException {
            return new KryoWriter(local.get(), new Output(os));
        }

        @Override
        protected ObjectReader createReader(final InputStream is, final Class clazz) throws IOException {
            return new KryoReader(local.get(), new Input(is));
        }

    }
}
