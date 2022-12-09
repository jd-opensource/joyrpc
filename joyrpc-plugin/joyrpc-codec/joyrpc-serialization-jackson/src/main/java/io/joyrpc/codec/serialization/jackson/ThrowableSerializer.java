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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.joyrpc.constants.Constants;

import java.io.IOException;

/**
 * ResponsePayload序列化
 */
public class ThrowableSerializer extends JsonSerializer<Throwable> {

    public static final JsonSerializer INSTANCE = new ThrowableSerializer();

    @Override
    public void serialize(final Throwable throwable, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        if (throwable == null) {
            gen.writeNull();
        } else {
            gen.writeStartObject();
            gen.writeStringField(Constants.ANNOTATION_TYPE, throwable.getClass().getName());
            gen.writeStringField(Constants.FIELD_MESSAGE, throwable.getMessage());
            if (throwable.getCause() != null && throwable.getCause() != throwable) {
                gen.writeObjectField(Constants.FIELD_CAUSE, throwable.getCause());
            }
            StackTraceElement[] traces = throwable.getStackTrace();
            if (traces != null) {
                gen.writeFieldName(Constants.FIELD_STACKTRACE);
                gen.writeStartArray();
                for (StackTraceElement trace : traces) {
                    gen.writeObject(trace);
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }
    }
}
