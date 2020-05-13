package io.joyrpc.protocol.dubbo.serialization.java;

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

import io.joyrpc.codec.serialization.ObjectReader;
import io.joyrpc.codec.serialization.ObjectWriter;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.Serializer;
import io.joyrpc.codec.serialization.java.JavaInputStream;
import io.joyrpc.codec.serialization.java.JavaSerialization;
import io.joyrpc.extension.Extension;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Dubbo的java序列化
 */
@Extension(value = "java", provider = "dubbo", order = Serialization.ORDER_DUBBO_JAVA)
public class DubboJavaSerialization extends JavaSerialization {

    @Override
    public byte getTypeId() {
        return DUBBO_JAVA_ID;
    }

    @Override
    public Serializer getSerializer() {
        return DubboJavaSerializer.INSTANCE;
    }

    protected static class DubboJavaSerializer extends JavaSerializer {

        protected static final JavaSerializer INSTANCE = new DubboJavaSerializer();

        @Override
        protected ObjectWriter createWriter(OutputStream os, Object object) throws IOException {
            return new DubboObjectOutputWriter(new ObjectOutputStream(os));
        }

        @Override
        protected ObjectReader createReader(InputStream is, Class clazz) throws IOException {
            return new DubboObjectInputReader(new JavaInputStream(is, BLACK_LIST));
        }
    }
}
