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
import io.joyrpc.protocol.message.Invocation;

import java.io.IOException;
import java.util.Map;

import static io.joyrpc.protocol.message.Invocation.*;

/**
 * Invocation序列化
 */
public class InvocationSerializer extends JsonSerializer<Invocation> {

    @Override
    public void serialize(final Invocation invocation, final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        if (invocation == null) {
            gen.writeNull();
        } else {
            gen.writeStartObject();
            gen.writeStringField(CLASS_NAME, invocation.getClassName());
            //2、alias
            gen.writeStringField(ALIAS, invocation.getAlias());
            //3、method name
            gen.writeStringField(METHOD_NAME, invocation.getMethodName());
            //4.argsType
            //TODO 应该根据泛型变量来决定是否要参数类型
            if (invocation.isCallback()) {
                //回调需要写上实际的参数类型
                gen.writeFieldName(ARGS_TYPE);
                String[] argsType = invocation.computeArgsType();
                gen.writeArray(argsType, 0, argsType.length);
            }
            //5、args
            gen.writeFieldName(ARGS);
            Object[] args = invocation.getArgs();
            if (args == null) {
                gen.writeNull();
            } else {
                gen.writeStartArray();
                for (Object arg : args) {
                    gen.writeObject(arg);
                }
                gen.writeEndArray();
            }
            //7、attachments
            Map<String, Object> attachments = invocation.getAttachments();
            if (attachments != null && !attachments.isEmpty()) {
                gen.writeFieldName(ATTACHMENTS);
                gen.writeObject(attachments);
            }
            gen.writeEndObject();
        }
    }
}
