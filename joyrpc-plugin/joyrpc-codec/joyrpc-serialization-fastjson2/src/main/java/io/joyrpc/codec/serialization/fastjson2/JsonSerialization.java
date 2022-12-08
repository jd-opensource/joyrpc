package io.joyrpc.codec.serialization.fastjson2;

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

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;
import io.joyrpc.codec.serialization.Json;
import io.joyrpc.codec.serialization.Serialization;
import io.joyrpc.codec.serialization.Serializer;
import io.joyrpc.codec.serialization.TypeReference;
import io.joyrpc.codec.serialization.fastjson2.java8.*;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.permission.BlackList;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.util.Resource.Definition;

import java.io.*;
import java.lang.reflect.Type;
import java.time.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.alibaba.fastjson2.JSONReader.Feature.IgnoreCheckClose;
import static io.joyrpc.context.Variable.VARIABLE;

/**
 * JSON序列化，不推荐在调用请求序列化场景使用
 */
@Extension(value = "json", provider = "fastjson2", order = Serialization.ORDER_FASTJSON)
@ConditionalOnClass("com.alibaba.fastjson2.JSON")
public class JsonSerialization implements Serialization, Json, BlackList.BlackListAware {

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
        return JsonSerializer.INSTANCE;
    }

    @Override
    public void writeJSONString(final OutputStream os, final Object object) throws SerializerException {
        JsonSerializer.INSTANCE.writeJSONString(os, object);
    }

    @Override
    public String toJSONString(final Object object) throws SerializerException {
        return JsonSerializer.INSTANCE.toJSONString(object);
    }

    @Override
    public byte[] toJSONBytes(final Object object) throws SerializerException {
        return JsonSerializer.INSTANCE.toJSONBytes(object);
    }

    @Override
    public <T> T parseObject(final String text, final Type type) throws SerializerException {
        return JsonSerializer.INSTANCE.parseObject(text, type);
    }

    @Override
    public <T> T parseObject(final String text, final TypeReference<T> reference) throws SerializerException {
        return JsonSerializer.INSTANCE.parseObject(text, reference);
    }

    @Override
    public <T> T parseObject(final InputStream is, final Type type) throws SerializerException {
        return JsonSerializer.INSTANCE.parseObject(is, type);
    }

    @Override
    public <T> T parseObject(final InputStream is, final TypeReference<T> reference) throws SerializerException {
        return JsonSerializer.INSTANCE.parseObject(is, reference);
    }

    @Override
    public void parseArray(final Reader reader, final Function<Function<Type, Object>, Boolean> function) throws SerializerException {
        JsonSerializer.INSTANCE.parseArray(reader, function);
    }

    @Override
    public void parseObject(final Reader reader, final BiFunction<String, Function<Type, Object>, Boolean> function) throws SerializerException {
        JsonSerializer.INSTANCE.parseObject(reader, function);
    }

    @Override
    public void updateBlack(final Collection<String> blackList) {
        JsonSerializer.BLACK_WHITE_LIST.updateBlack(blackList);
    }

    /**
     * JSON序列化和反序列化实现，有多种JSON序列化框架，把Fastjson的异常进行转换
     */
    protected static class JsonSerializer implements Serializer, Json {

        protected static final ObjectReaderBlackWhiteList BLACK_WHITE_LIST = new ObjectReaderBlackWhiteList(
                new Definition[]{
                        new Definition("permission/fastjson.blacklist"),
                        new Definition("META-INF/permission/fastjson.blacklist", true)});

        protected static final JsonSerializer INSTANCE = new JsonSerializer();

        protected ObjectWriterProvider objectWriterProvider;
        protected JSONWriter.Context writerContext;
        protected ObjectReaderProvider objectReaderProvider;
        protected JSONReader.Context readerContext;
        protected JSONReader.Feature[] parserFeatures;
        protected JSONWriter.Feature[] serializerFeatures;

        protected JsonSerializer() {
            objectWriterProvider = new ObjectWriterProvider();
            regsiter(objectWriterProvider);
            writerContext = JSONFactory.createWriteContext(objectWriterProvider);
            writerContext.setDateFormat("iso8601");
            objectReaderProvider = new SecurityObjectReaderProvider(BLACK_WHITE_LIST);
            regsiter(objectReaderProvider);
            readerContext = JSONFactory.createReadContext(objectReaderProvider);
            parserFeatures = createReaderFeatures();
            serializerFeatures = createWriterFeatures();
        }

        /**
         * 创建序列化配置
         *
         * @return
         */
        protected void regsiter(final ObjectReaderProvider provider) {
            provider.register(MonthDay.class, MonthDaySerialization.INSTANCE);
            provider.register(YearMonth.class, YearMonthSerialization.INSTANCE);
            provider.register(Year.class, YearSerialization.INSTANCE);
            provider.register(ZoneOffset.class, ZoneOffsetSerialization.INSTANCE);
            provider.register(ZoneId.class, ZoneIdSerialization.INSTANCE);
            provider.register(ZoneId.systemDefault().getClass(), ZoneIdSerialization.INSTANCE);
            provider.register(Invocation.class, InvocationSerialization.INSTANCE);
            provider.register(ResponsePayload.class, ResponsePayloadSerialization.INSTANCE);
            //provider.register(GregorianCalendar.class,Calendarcod)
        }

        /**
         * 创建序列化配置
         *
         * @return
         */
        protected void regsiter(final ObjectWriterProvider provider) {
            provider.register(MonthDay.class, MonthDaySerialization.INSTANCE);
            provider.register(YearMonth.class, YearMonthSerialization.INSTANCE);
            provider.register(Year.class, YearSerialization.INSTANCE);
            provider.register(ZoneOffset.class, ZoneOffsetSerialization.INSTANCE);
            provider.register(ZoneId.class, ZoneIdSerialization.INSTANCE);
            provider.register(ZoneId.systemDefault().getClass(), ZoneIdSerialization.INSTANCE);
            provider.register(Invocation.class, InvocationSerialization.INSTANCE);
            //provider.register(ResponsePayload.class, ResponsePayloadCodec.INSTANCE);
        }

        /**
         * 添加解析Feature
         *
         * @param features
         */
        protected void addReaderFeature(Set<JSONReader.Feature> features) {

        }

        /**
         * 构造反序列化特征
         *
         * @return
         */
        protected JSONReader.Feature[] createReaderFeatures() {
            HashSet<JSONReader.Feature> set = new HashSet<>();
            //从上下文中读取
            String cfg = VARIABLE.getString("json.parser.features");
            if (cfg != null && !cfg.isEmpty()) {
                String[] features = cfg.split("[,;\\s]");
                for (String feature : features) {
                    try {
                        set.add(JSONReader.Feature.valueOf(feature));
                    } catch (IllegalArgumentException e) {
                    }
                }

            }
            addReaderFeature(set);
            if (!set.isEmpty()) {
                return set.toArray(new JSONReader.Feature[set.size()]);
            }
            return new JSONReader.Feature[0];
        }

        /**
         * 添加序列化Feature
         *
         * @param features
         */
        protected void addWriterFeature(Set<JSONWriter.Feature> features) {
            features.add(JSONWriter.Feature.WriteNonStringKeyAsString);
        }

        /**
         * 构造序列化特征
         *
         * @return
         */
        protected JSONWriter.Feature[] createWriterFeatures() {
            HashSet<JSONWriter.Feature> set = new HashSet<>();
            String cfg = VARIABLE.getString("json.serializer.features");
            if (cfg != null && !cfg.isEmpty()) {
                String[] features = cfg.split("[,;\\s]");
                for (String feature : features) {
                    try {
                        set.add(JSONWriter.Feature.valueOf(feature));
                    } catch (IllegalArgumentException ignored) {
                    }
                }

            }
            addWriterFeature(set);
            if (!set.isEmpty()) {
                return set.toArray(new JSONWriter.Feature[set.size()]);
            }
            return new JSONWriter.Feature[0];
        }

        protected <T> T toJSON(final Object object, Function<JSONWriter, T> function) throws SerializerException {
            try (JSONWriter writer = JSONWriter.ofUTF8(writerContext)) {
                if (object == null) {
                    writer.writeNull();
                } else {
                    writer.setRootObject(object);
                    Class<?> valueClass = object.getClass();
                    ObjectWriter<?> objectWriter = writer.getObjectWriter(valueClass, valueClass);
                    objectWriter.write(writer, object, null, null, 0);
                }
                return function.apply(writer);
            } catch (SerializerException e) {
                throw e;
            } catch (JSONException e) {
                throw new SerializerException("Error occurs while serializing object,caused by " + e.getMessage(),
                        e.getCause() != null ? e.getCause() : null);
            } catch (Exception e) {
                throw new SerializerException("Error occurs while serializing object,caused by " + e.getMessage(), e);
            }
        }

        protected <T> T parse(final Reader reader, final Function<JSONReader, T> function) throws SerializerException {
            if (reader == null) {
                return null;
            }
            try (JSONReader jsonReader = JSONReader.of(reader, readerContext)) {
                if (jsonReader.isEnd()) {
                    return null;
                }
                return function.apply(jsonReader);
            } catch (SerializerException e) {
                throw e;
            } catch (JSONException e) {
                throw new SerializerException("Error occurs while deserializing object,caused by " + e.getMessage(),
                        e.getCause() != null ? e.getCause() : null);
            } catch (Exception e) {
                throw new SerializerException("Error occurs while deserializing object,caused by " + e.getMessage(), e);
            }
        }

        protected <T> T parse(final Reader reader, final Type type, final BiFunction<JSONReader, Type, T> function) throws SerializerException {
            return parse(reader, r -> function.apply(r, type));
        }

        protected <T> T parse(final JSONReader reader, final Type type) {
            ObjectReader<T> objectReader = reader.getObjectReader(type);

            T object = objectReader.readObject(reader, null, null, 0);
            if (!reader.isEnd() && (readerContext.getFeatures() & IgnoreCheckClose.mask) == 0) {
                throw new JSONException(reader.info("input not end"));
            }
            return object;
        }

        @Override
        public <T> void serialize(final OutputStream os, final T object) throws SerializerException {
            toJSON(object, (writer) -> {
                try {
                    writer.flushTo(os);
                    return null;
                } catch (IOException e) {
                    throw new SerializerException("Error occurs while serializing object,caused by " + e.getMessage(), e);
                }
            });
        }

        @Override
        public <T> T deserialize(final InputStream is, final Type type) throws SerializerException {
            return is == null ? null : parse(new InputStreamReader(is), type, this::parse);
        }

        @Override
        public void writeJSONString(final OutputStream os, final Object object) throws SerializerException {
            serialize(os, object);
        }

        @Override
        public String toJSONString(final Object object) throws SerializerException {
            return toJSON(object, JSONWriter::toString);
        }

        @Override
        public byte[] toJSONBytes(final Object object) throws SerializerException {
            return toJSON(object, JSONWriter::getBytes);
        }

        @Override
        public <T> T parseObject(final String text, final Type type) throws SerializerException {
            return text == null ? null : parse(new StringReader(text), type, this::parse);
        }

        @Override
        public <T> T parseObject(final InputStream is, final Type type) throws SerializerException {
            return is == null ? null : parse(new InputStreamReader(is), type, this::parse);
        }

        @Override
        public <T> T parseObject(final InputStream is, final TypeReference<T> reference) throws SerializerException {
            return is == null ? null : parse(new InputStreamReader(is), reference == null ? null : reference.getType(), this::parse);
        }

        @Override
        public <T> T parseObject(final String text, final TypeReference<T> reference) throws SerializerException {
            return text == null ? null : parse(new StringReader(text), reference == null ? null : reference.getType(), this::parse);
        }

        @Override
        public void parseArray(final Reader reader, final Function<Function<Type, Object>, Boolean> function) throws SerializerException {
            parse(reader, (Function<JSONReader, Void>) r -> {
                if (!r.isArray()) {
                    return null;
                }
                r.startArray();
                while (!r.isEnd()) {
                    if (!function.apply(o -> parse(r, o))) {
                        break;
                    }
                }
                r.endArray();
                return null;
            });
        }

        @Override
        public void parseObject(final Reader reader, final BiFunction<String, Function<Type, Object>, Boolean> function) throws SerializerException {
            parse(reader, (Function<JSONReader, Void>) r -> {
                if (!r.isObject()) {
                    return null;
                }
                r.nextIfObjectStart();
                while (!r.isEnd()) {
                    if (!function.apply(r.readFieldName(), type -> parse(r, type))) {
                        break;
                    }
                }
                r.nextIfObjectEnd();
                return null;
            });
        }
    }


}
