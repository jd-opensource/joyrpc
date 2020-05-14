package io.joyrpc.codec.serialization.java;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Java序列化
 */
@Extension(value = "java", provider = "advance", order = Serialization.ORDER_ADVANCE_JAVA)
public class AdvanceJavaSerialization extends JavaSerialization {

    @Override
    public byte getTypeId() {
        return ADVANCE_JAVA_ID;
    }

    @Override
    public Serializer getSerializer() {
        return AdvanceJavaSerializer.INSTANCE;
    }


    /**
     * Java序列化和反序列化实现
     */
    protected static class AdvanceJavaSerializer extends JavaSerializer {

        protected static final AdvanceJavaSerializer INSTANCE = new AdvanceJavaSerializer();

        protected AdvanceJavaSerializer() {
        }

        @Override
        protected ObjectWriter createWriter(final OutputStream os, final Object object) throws IOException {
            return new AdvanceObjectOutputWriter(new ObjectOutputStream(os));
        }

        @Override
        protected ObjectReader createReader(final InputStream is, final Class clazz) throws IOException {
            return new AdvanceObjectInputReader(new JavaInputStream(is, BLACK_LIST));
        }

    }
}
