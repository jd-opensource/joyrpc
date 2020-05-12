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

import com.esotericsoftware.kryo.AutowiredObjectSerializer;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import de.javakaffee.kryoserializers.*;
import io.joyrpc.codec.serialization.*;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * kryo
 */
@Extension(value = "kryo", provider = "esotericsoftware", order = Serialization.ORDER_KRYO)
@ConditionalOnClass({"com.esotericsoftware.kryo.Kryo", "de.javakaffee.kryoserializers.JdkProxySerializer"})
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
        /**
         * 绑定在线程变量里面
         */
        protected static final ThreadLocal<Kryo> local = ThreadLocal.withInitial(() -> {
            final Kryo kryo = new Kryo();
            kryo.addDefaultSerializer(Throwable.class, new JavaSerializer());
            kryo.register(Arrays.asList("").getClass(), new ArraysAsListSerializer());
            kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
            kryo.register(InvocationHandler.class, new JdkProxySerializer());
            kryo.register(BigDecimal.class, new DefaultSerializers.BigDecimalSerializer());
            kryo.register(BigInteger.class, new DefaultSerializers.BigIntegerSerializer());
            kryo.register(Pattern.class, new RegexSerializer());
            kryo.register(BitSet.class, new BitSetSerializer());
            kryo.register(URI.class, new URISerializer());
            kryo.register(UUID.class, new UUIDSerializer());
            UnmodifiableCollectionsSerializer.registerSerializers(kryo);
            SynchronizedCollectionsSerializer.registerSerializers(kryo);

            // now just added some very common classes
            kryo.register(HashMap.class);
            kryo.register(ArrayList.class);
            kryo.register(LinkedList.class);
            kryo.register(HashSet.class);
            kryo.register(TreeSet.class);
            kryo.register(Hashtable.class);
            kryo.register(Date.class);
            kryo.register(Calendar.class);
            kryo.register(ConcurrentHashMap.class);
            kryo.register(SimpleDateFormat.class);
            kryo.register(GregorianCalendar.class);
            kryo.register(Vector.class);
            kryo.register(BitSet.class);
            kryo.register(StringBuffer.class);
            kryo.register(StringBuilder.class);
            kryo.register(Object.class);
            kryo.register(Object[].class);
            kryo.register(String[].class);
            kryo.register(byte[].class);
            kryo.register(char[].class);
            kryo.register(int[].class);
            kryo.register(float[].class);
            kryo.register(double[].class);
            //注册插件，便于第三方协议注册序列化实现
            register(AutowiredObjectSerializer.class, o -> kryo.addDefaultSerializer(o.getType(), o));
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
