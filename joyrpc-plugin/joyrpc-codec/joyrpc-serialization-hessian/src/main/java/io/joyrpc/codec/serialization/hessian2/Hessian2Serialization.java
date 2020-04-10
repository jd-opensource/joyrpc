package io.joyrpc.codec.serialization.hessian2;

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
import io.joyrpc.com.caucho.hessian.io.Hessian2Input;
import io.joyrpc.com.caucho.hessian.io.Hessian2Output;
import io.joyrpc.com.caucho.hessian.io.SerializerFactory;
import io.joyrpc.extension.Extension;
import io.joyrpc.permission.BlackList;
import io.joyrpc.util.ClassUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * hessian2序列化协议
 */
@Extension(value = "hessian", provider = "caucho", order = Serialization.ORDER_HESSIAN)
public class Hessian2Serialization implements Serialization, BlackList.BlackListAware {

    @Override
    public byte getTypeId() {
        return HESSIAN_ID;
    }

    @Override
    public String getContentType() {
        return "application/x-hessian";
    }

    @Override
    public Serializer getSerializer() {
        return Hessian2Serializer.INSTANCE;
    }

    @Override
    public void updateBlack(final Collection<String> blackList) {
        Hessian2Serializer.BLACK_LIST.updateBlack(blackList);
    }

    /**
     * Hessian2序列化和反序列化实现
     */
    protected static final class Hessian2Serializer extends AbstractSerializer {

        protected static final BlackList<String> BLACK_LIST = new SerializerBlackList("permission/hessian.blacklist",
                "META-INF/permission/hessian.blacklist").load();

        protected static final SerializerFactory SERIALIZER_FACTORY = new SerializerFactory(ClassUtils.getClassLoader(Hessian2Serialization.class));

        protected static final Hessian2Serializer INSTANCE = new Hessian2Serializer();

        static {
            SERIALIZER_FACTORY.setAllowNonSerializable(true);
            SERIALIZER_FACTORY.addFactory(new Java8SerializerFactory());
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
