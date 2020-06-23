package io.joyrpc.codec.serialization.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.joyrpc.codec.serialization.*;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.permission.BlackList;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collection;
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
            } catch (Exception e) {
                throw new SerializerException("Error occurred while serializing object", e);
            }
        }

        @Override
        public <T> T parseObject(final String text, final Type type) throws SerializerException {
            if (text == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(text, new com.fasterxml.jackson.core.type.TypeReference<Object>() {
                    @Override
                    public Type getType() {
                        return type;
                    }
                });
            } catch (Exception e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public <T> T parseObject(final String text, final TypeReference<T> reference) throws SerializerException {
            if (text == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(text, new com.fasterxml.jackson.core.type.TypeReference<Object>() {
                    @Override
                    public Type getType() {
                        return reference.getType();
                    }
                });
            } catch (Exception e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public <T> T parseObject(final InputStream is, Type type) throws SerializerException {
            if (is == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<Object>() {
                    @Override
                    public Type getType() {
                        return type;
                    }
                });
            } catch (Exception e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public <T> T parseObject(final InputStream is, final TypeReference<T> reference) throws SerializerException {
            if (is == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<Object>() {
                    @Override
                    public Type getType() {
                        return reference.getType();
                    }
                });
            } catch (Exception e) {
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
            } catch (SerializerException e) {
                throw e;
            } catch (Exception e) {
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
            } catch (SerializerException e) {
                throw e;
            } catch (Exception e) {
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
                return parser.readValueAs(new com.fasterxml.jackson.core.type.TypeReference<Object>() {
                    @Override
                    public Type getType() {
                        return type;
                    }
                });
            } catch (IOException e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }

        @Override
        public <T> void serialize(final OutputStream os, final T object) throws SerializerException {
            try {
                mapper.writeValue(os, object);
            } catch (Exception e) {
                throw new SerializerException("Error occurred serializing object", e);
            }
        }

        @Override
        public <T> T deserialize(final InputStream is, final Type type) throws SerializerException {
            if (is == null) {
                return null;
            }
            try {
                return (T) mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<Object>() {
                    @Override
                    public Type getType() {
                        return type;
                    }
                });
            } catch (Exception e) {
                throw new SerializerException("Error occurs while parsing object", e);
            }
        }
    }
}
