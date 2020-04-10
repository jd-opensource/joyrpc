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
import io.joyrpc.permission.BlackList;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * Java序列化
 */
@Extension(value = "java", provider = "java", order = Serialization.ORDER_JAVA)
public class JavaSerialization implements Serialization, BlackList.BlackListAware {

    @Override
    public byte getTypeId() {
        return JAVA_ID;
    }

    @Override
    public String getContentType() {
        return "application/x-java";
    }

    @Override
    public Serializer getSerializer() {
        return JavaSerializer.INSTANCE;
    }

    @Override
    public void updateBlack(final Collection<String> blackList) {
        JavaSerializer.BLACK_LIST.updateBlack(blackList);
    }

    /**
     * Java序列化和反序列化实现
     */
    protected static final class JavaSerializer extends AbstractSerializer {

        protected static final BlackList<String> BLACK_LIST = new SerializerBlackList("permission/java.blacklist",
                "META-INF/permission/java.blacklist").load();

        protected static final JavaSerializer INSTANCE = new JavaSerializer();

        protected JavaSerializer() {
        }

        @Override
        protected ObjectWriter createWriter(final OutputStream os, final Object object) throws IOException {
            return new ObjectOutputWriter(new ObjectOutputStream(os));
        }

        @Override
        protected ObjectReader createReader(final InputStream is, final Class clazz) throws IOException {
            return new ObjectInputReader(new JavaInputStream(is, BLACK_LIST));
        }

    }
}
