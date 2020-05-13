package io.joyrpc.protocol.dubbo.serialization.hessian2;

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
import io.joyrpc.codec.serialization.AbstractSerializer;
import io.joyrpc.codec.serialization.Serializer;
import io.joyrpc.codec.serialization.hessian2.*;
import io.joyrpc.com.caucho.hessian.io.*;
import io.joyrpc.extension.Extension;
import io.joyrpc.permission.BlackList;
import io.joyrpc.util.ClassUtils;

import java.io.InputStream;
import java.io.OutputStream;

@Extension(value = "hessian", provider = "dubbo", order = Serialization.ORDER_HESSIAN)
public class DubboHessian2Serialization extends Hessian2Serialization {

    public static byte DUBBO_HESSIAN_ID = 30;

    @Override
    public byte getTypeId() {
        return DUBBO_HESSIAN_ID;
    }

    @Override
    public Serializer getSerializer() {
        return DubboHessian2Serializer.INSTANCE;
    }

    /**
     * Hessian2序列化和反序列化实现
     */
    protected static final class DubboHessian2Serializer extends AbstractSerializer {

        protected static final BlackList<String> BLACK_LIST = new SerializerBlackList("permission/hessian.blacklist",
                "META-INF/permission/hessian.blacklist").load();

        protected static final SerializerFactory SERIALIZER_FACTORY = new SerializerFactory(ClassUtils.getClassLoader(DubboHessian2Serialization.class));

        protected static final DubboHessian2Serializer INSTANCE = new DubboHessian2Serializer();

        static {
            SERIALIZER_FACTORY.setAllowNonSerializable(true);
            DubboHessian2SerializerFactory factory = new DubboHessian2SerializerFactory();
            register(AutowiredObjectSerializer.class, o -> factory.serializers.put(o.getType(), o));
            register(AutowiredObjectDeserializer.class, o -> factory.deserializers.put(o.getType(), o));
            if (factory.deserializers.isEmpty()) {
                factory.deserializers = null;
            }
            SERIALIZER_FACTORY.addFactory(factory);
        }

        @Override
        protected ObjectWriter createWriter(final OutputStream os, final Object object) {
            Hessian2Output hessian2Output = new io.joyrpc.com.caucho.hessian.io.Hessian2Output(os);
            hessian2Output.setSerializerFactory(SERIALIZER_FACTORY);
            return new Hessian2Writer(hessian2Output);
        }

        @Override
        protected ObjectReader createReader(final InputStream is, final Class clazz) {
            Hessian2Input hessian2Input = new Hessian2BWLInput(is, BLACK_LIST);
            hessian2Input.setSerializerFactory(SERIALIZER_FACTORY);
            return new Hessian2Reader(hessian2Input, is);
        }
    }
}
