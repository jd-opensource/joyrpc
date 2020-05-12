package io.joyrpc.codec.serialization.fst;

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
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import org.nustaq.serialization.AutowiredObjectSerializer;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * fast-serialization
 */
@Extension(value = "fst", provider = "nustaq", order = Serialization.ORDER_FST)
@ConditionalOnClass("org.nustaq.serialization.FSTConfiguration")
public class FSTSerialization implements Serialization {

    @Override
    public byte getTypeId() {
        return FST_ID;
    }

    @Override
    public String getContentType() {
        return "application/x-fst";
    }

    @Override
    public Serializer getSerializer() {
        return FSTSerializer.INSTANCE;
    }

    /**
     * FST序列化和反序列化实现
     */
    protected static final class FSTSerializer extends AbstractSerializer {

        /**
         * 单例，延迟加载
         */
        protected static final FSTSerializer INSTANCE = new FSTSerializer();
        /**
         * FST配置
         */
        protected static FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();

        static {
            //注册插件，便于第三方协议注册序列化实现
            register(AutowiredObjectSerializer.class, o -> fst.registerSerializer(o.getType(), o, false));
        }

        protected FSTSerializer() {
        }

        @Override
        protected ObjectWriter createWriter(final OutputStream os, final Object object) throws IOException {
            return new FSTObjectWriter(fst.getObjectOutput(os));
        }

        @Override
        protected ObjectReader createReader(final InputStream is, final Class clazz) throws IOException {
            return new FSTObjectReader(fst, is);
        }

    }
}
