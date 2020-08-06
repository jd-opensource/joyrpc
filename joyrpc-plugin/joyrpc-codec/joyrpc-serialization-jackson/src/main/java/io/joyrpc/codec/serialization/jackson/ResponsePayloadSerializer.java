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
import io.joyrpc.protocol.message.ResponsePayload;

import java.io.IOException;
import java.lang.reflect.Type;

import static io.joyrpc.protocol.message.ResponsePayload.RESPONSE;
import static io.joyrpc.protocol.message.ResponsePayload.RES_CLASS;
import static io.joyrpc.util.ClassUtils.getCanonicalName;

/**
 * ResponsePayload序列化
 */
public class ResponsePayloadSerializer extends JsonSerializer<ResponsePayload> {

    public static final JsonSerializer INSTANCE = new ResponsePayloadSerializer();

    @Override
    public void serialize(final ResponsePayload payload, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        if (payload == null) {
            gen.writeNull();
        } else {
            gen.writeStartObject();
            Throwable exception = payload.getException();
            Object response = payload.getResponse();
            if (response != null) {
                gen.writeStringField(RES_CLASS, getTypeName(response, payload.getType()));
                gen.writeObjectField(RESPONSE, response);
            } else if (exception != null) {
                gen.writeStringField(RES_CLASS, getCanonicalName(exception.getClass()));
                gen.writeObjectField(RESPONSE, exception);
            }
            gen.writeEndObject();
        }
    }

    /**
     * 获取应答的类型名称
     *
     * @param response 应答
     * @param type     类型
     * @return 类型名称
     */
    protected String getTypeName(final Object response, final Type type) {
        return type == null ? getCanonicalName(response.getClass()) : type.getTypeName();
    }
}
