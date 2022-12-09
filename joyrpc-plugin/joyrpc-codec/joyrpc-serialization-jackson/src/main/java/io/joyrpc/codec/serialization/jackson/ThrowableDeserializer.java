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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import io.joyrpc.exception.CreationException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.util.ClassUtils;

import java.io.IOException;

import static io.joyrpc.constants.Constants.*;

/**
 * 异常解析
 */
public class ThrowableDeserializer extends AbstractDeserializer<Throwable> {

    public static final JsonDeserializer INSTANCE = new ThrowableDeserializer();

    @Override
    public Throwable deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JsonProcessingException {
        switch (parser.currentToken()) {
            case VALUE_NULL:
                return null;
            case START_OBJECT:
                return parse(parser);
            default:
                throw new SerializerException("Error occurs while parsing throwable.");
        }
    }

    /**
     * 解析
     *
     * @param parser 解析器
     * @return 调用对象
     * @throws IOException
     */
    protected Throwable parse(final JsonParser parser) throws IOException {
        Class<?> clazz = null;
        Throwable cause = null;
        String message = null;
        StackTraceElement[] stackTrace = null;
        try {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                switch (parser.currentName()) {
                    case ANNOTATION_TYPE -> {
                        String className = readString(parser, ANNOTATION_TYPE, true);
                        if (className != null) {
                            clazz = ClassUtils.forName(className);
                        }
                    }
                    case FIELD_CAUSE -> cause = parseCause(parser);
                    case FIELD_MESSAGE -> message = readString(parser, FIELD_MESSAGE, true);
                    case FIELD_LOCALIZE_MESSAGE -> readString(parser, FIELD_LOCALIZE_MESSAGE, true);
                    case FIELD_STACKTRACE -> stackTrace = parseTrace(parser);
                }
            }
            return ClassUtils.createException(clazz, message, cause, stackTrace);
        } catch (CreationException | ClassNotFoundException e) {
            throw new SerializerException(e.getMessage(), e.getCause());
        }
    }

    /**
     * 解析根因
     *
     * @param parser
     * @return
     * @throws IOException
     */
    protected Throwable parseCause(final JsonParser parser) throws IOException {
        switch (parser.nextToken()) {
            case START_OBJECT:
                return parser.readValueAs(Throwable.class);
            case VALUE_NULL:
                return null;
            default:
                throw new SerializerException("syntax error: cause is illegal.");

        }
    }

    /**
     * 解析堆栈
     *
     * @param parser
     * @return
     * @throws IOException
     */
    protected StackTraceElement[] parseTrace(final JsonParser parser) throws IOException {
        switch (parser.nextToken()) {
            case START_ARRAY:
                return parser.readValueAs(StackTraceElement[].class);
            case VALUE_NULL:
                return null;
            default:
                throw new SerializerException("syntax error: stackTrace is illegal.");

        }
    }
}
