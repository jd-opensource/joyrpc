package io.joyrpc.codec.serialization.jackson;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.joyrpc.codec.serialization.*;
import io.joyrpc.codec.serialization.jackson.java8.*;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.Option;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.permission.BlackList;
import io.joyrpc.protocol.message.Invocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.time.*;
import java.util.Collection;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;

@Extension(value = "json", provider = "jackson", order = Serialization.ORDER_JACKSON)
@ConditionalOnClass("com.fasterxml.jackson.core.JsonFactory")
public class JacksonSerialization implements Serialization, Json, BlackList.BlackListAware {

    @Override
    public byte getTypeId() {
        return JSON_ID;
    }

    @Override
    public String getContentType() {
        return "text/json";
    }

    @Override
    public boolean autoType() {
        //在序列化Invocation的调用参数时候不支持类型，需要类名
        return false;
    }

    @Override
    public Serializer getSerializer() {
        return JacksonnSerializer.INSTANCE;
    }

    @Override
    public void writeJSONString(final OutputStream os, final Object object) throws SerializerException {
        JacksonnSerializer.INSTANCE.writeJSONString(os, object);
    }

    @Override
    public String toJSONString(final Object object) throws SerializerException {
        return JacksonnSerializer.INSTANCE.toJSONString(object);
    }

    @Override
    public byte[] toJSONBytes(final Object object) throws SerializerException {
        return JacksonnSerializer.INSTANCE.toJSONBytes(object);
    }

    @Override
    public <T> T parseObject(final String text, final Type type) throws SerializerException {
        return JacksonnSerializer.INSTANCE.parseObject(text, type);
    }

    @Override
    public <T> T parseObject(final String text, final TypeReference<T> reference) throws SerializerException {
        return JacksonnSerializer.INSTANCE.parseObject(text, reference);
    }

    @Override
    public <T> T parseObject(final InputStream is, final Type type) throws SerializerException {
        return JacksonnSerializer.INSTANCE.parseObject(is, type);
    }

    @Override
    public <T> T parseObject(final InputStream is, final TypeReference<T> reference) throws SerializerException {
        return JacksonnSerializer.INSTANCE.parseObject(is, reference);
    }

    @Override
    public void parseArray(final Reader reader, final Function<Function<Type, Object>, Boolean> function) throws SerializerException {
        JacksonnSerializer.INSTANCE.parseArray(reader, function);
    }

    @Override
    public void parseObject(final Reader reader, final BiFunction<String, Function<Type, Object>, Boolean> function) throws SerializerException {
        JacksonnSerializer.INSTANCE.parseObject(reader, function);
    }

    @Override
    public void updateBlack(final Collection<String> blackList) {
        JacksonnSerializer.BLACK_LIST.updateBlack(blackList);
    }

    /**
     * JSON序列化和反序列化实现
     */
    protected static class JacksonnSerializer implements Serializer, Json {

        protected static final BlackList<String> BLACK_LIST = new SerializerBlackList("permission/jackson.blacklist",
                "META-INF/permission/jackson.blacklist").load();
        protected static final JacksonnSerializer INSTANCE = new JacksonnSerializer();

        protected ObjectMapper mapper = new ObjectMapper();

        public JacksonnSerializer() {
            SimpleModule module = new SimpleModule();
            //TODO 增加java8的序列化
            module.addSerializer(Invocation.class, InvocationSerializer.INSTANCE);
            module.addSerializer(Duration.class, DurationSerializer.INSTANCE);
            module.addSerializer(Instant.class, InstantSerializer.INSTANCE);
            module.addSerializer(LocalDateTime.class, LocalDateTimeSerializer.INSTANCE);
            module.addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE);
            module.addSerializer(LocalTime.class, LocalTimeSerializer.INSTANCE);
            module.addSerializer(OffsetDateTime.class, OffsetDateTimeSerializer.INSTANCE);
            module.addSerializer(OffsetTime.class, OffsetTimeSerializer.INSTANCE);
            module.addSerializer(Option.class, OptionalSerializer.INSTANCE);
            module.addSerializer(OptionalDouble.class, OptionalDoubleSerializer.INSTANCE);
            module.addSerializer(OptionalInt.class, OptionalIntSerializer.INSTANCE);
            module.addSerializer(OptionalLong.class, OptionalLongSerializer.INSTANCE);
            module.addSerializer(Period.class, PeriodSerializer.INSTANCE);
            module.addSerializer(ZonedDateTime.class, ZonedDateTimeSerializer.INSTANCE);
            module.addSerializer(ZoneOffset.class, ZoneOffsetSerializer.INSTANCE);
            module.addDeserializer(Invocation.class, InvocationDeserializer.INSTANCE);
            module.addDeserializer(Duration.class, DurationDeserializer.INSTANCE);
            module.addDeserializer(Instant.class, InstantDeserializer.INSTANCE);
            module.addDeserializer(LocalDateTime.class, LocalDateTimeDeserializer.INSTANCE);
            module.addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE);
            module.addDeserializer(LocalTime.class, LocalTimeDeserializer.INSTANCE);
            module.addDeserializer(OffsetDateTime.class, OffsetDateTimeDeserializer.INSTANCE);
            module.addDeserializer(OffsetTime.class, OffsetTimeDeserializer.INSTANCE);
            module.addDeserializer(Option.class, OptionalDeserializer.INSTANCE);
            module.addDeserializer(OptionalDouble.class, OptionalDoubleDeserializer.INSTANCE);
            module.addDeserializer(OptionalInt.class, OptionalIntDeserializer.INSTANCE);
            module.addDeserializer(OptionalLong.class, OptionalLongDeserializer.INSTANCE);
            module.addDeserializer(Period.class, PeriodDeserializer.INSTANCE);
            module.addDeserializer(ZonedDateTime.class, ZonedDateTimeDeserializer.INSTANCE);
            module.addDeserializer(ZoneOffset.class, ZoneOffsetDeserializer.INSTANCE);
            mapper.registerModule(module);
        }

        @Override
        public void writeJSONString(final OutputStream os, final Object object) throws SerializerException {
            try {
                mapper.writeValue(os, object);
            } catch (IOException e) {
                throw new SerializerException("Error occurred while serializing object", e);
            }
        }

        @Override
        public String toJSONString(final Object object) throws SerializerException {
            try {
                return mapper.writeValueAsString(object);
            } catch (IOException e) {
                throw new SerializerException("Error occurred while serializing object", e);
            }
        }

        @Override
        public byte[] toJSONBytes(final Object object) throws SerializerException {
            try {
                UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream();
                mapper.writeValue(baos, object);
                return baos.toByteArray();
            } catch (IOException e) {
                throw new SerializerException("Error occurred while serializing object", e);
            }
        }

        @Override
        public <T> T parseObject(final String text, final Type type) throws SerializerException {
            if (text == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(text, new SimpleTypeReference(type));
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public <T> T parseObject(final String text, final TypeReference<T> reference) throws SerializerException {
            if (text == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(text, new SimpleTypeReference(reference.getType()));
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public <T> T parseObject(final InputStream is, Type type) throws SerializerException {
            if (is == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(is, new SimpleTypeReference(type));
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public <T> T parseObject(final InputStream is, final TypeReference<T> reference) throws SerializerException {
            if (is == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(is, new SimpleTypeReference(reference.getType()));
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public void parseArray(final Reader reader, final Function<Function<Type, Object>, Boolean> function) throws SerializerException {
            try {
                JsonParser parser = mapper.createParser(reader);
                // loop until token equal to "}"
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (!function.apply(o -> parseObject(parser, o))) {
                        break;
                    }
                }
                parser.close();
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public void parseObject(final Reader reader, final BiFunction<String, Function<Type, Object>, Boolean> function) throws SerializerException {
            try {
                JsonParser parser = mapper.createParser(reader);
                // loop until token equal to "}"
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    if (!function.apply(parser.getCurrentName(), o -> parseObject(parser, o))) {
                        break;
                    }
                }
                parser.close();
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        /**
         * 根据parser进行解析
         *
         * @param parser 解析器
         * @param type   类型
         * @return 对象
         */
        protected Object parseObject(final JsonParser parser, final Type type) {
            try {
                return parser.readValueAs(new SimpleTypeReference(type));
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public <T> void serialize(final OutputStream os, final T object) throws SerializerException {
            try {
                mapper.writeValue(os, object);
            } catch (IOException e) {
                throw new SerializerException("Error occurred serializing object", e);
            }
        }

        @Override
        public <T> T deserialize(final InputStream is, final Type type) throws SerializerException {
            if (is == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(is, new SimpleTypeReference(type));
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }
    }

}
