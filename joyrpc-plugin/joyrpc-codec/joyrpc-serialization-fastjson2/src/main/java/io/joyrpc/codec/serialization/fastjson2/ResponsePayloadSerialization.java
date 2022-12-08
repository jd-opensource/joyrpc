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

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;
import io.joyrpc.util.ClassUtils;

import java.lang.reflect.Type;

import static io.joyrpc.protocol.message.ResponsePayload.*;
import static io.joyrpc.util.ClassUtils.getCanonicalName;
import static io.joyrpc.util.GenericMethod.getReturnGenericType;

/**
 * 应答序列化
 */
public class ResponsePayloadSerialization extends AbstractSerialization<ResponsePayload> {

    public static final ResponsePayloadSerialization INSTANCE = new ResponsePayloadSerialization();

    @Override
    protected void doWrite(final JSONWriter jsonWriter, final Object object, final Object fieldName, final Type fieldType, final long features) {
        writeObjectBegin(jsonWriter);
        ResponsePayload payload = (object instanceof ResponseMessage ? ((ResponseMessage<ResponsePayload>) object).getPayLoad() : (ResponsePayload) object);
        if (payload != null) {
            Throwable exception = payload.getException();
            Object response = payload.getResponse();
            if (response != null) {
                writeString(jsonWriter, RES_CLASS, getTypeName(response, payload.getType()), false);
                writeObject(jsonWriter, RESPONSE, response, false);
            } else if (exception != null) {
                writeString(jsonWriter, RES_CLASS, getCanonicalName(exception.getClass()), false);
                writeObject(jsonWriter, EXCEPTION, exception, false);
            }
        }
        writeObjectEnd(jsonWriter);
    }

    @Override
    protected ResponsePayload doRead(final JSONReader jsonReader, final Type fieldType, final Object fieldName, final long features) {
        ResponsePayload payload = new ResponsePayload();
        String key;
        String typeName = null;
        try {
            jsonReader.nextIfObjectStart();
            while (!jsonReader.isEnd() && !jsonReader.nextIfObjectEnd()) {
                key = jsonReader.readFieldName();
                if (RES_CLASS.equals(key)) {
                    typeName = readString(jsonReader, RES_CLASS, false);
                } else if (RESPONSE.equals(key)) {
                    payload.setResponse(parseResponse(jsonReader, typeName));
                } else if (EXCEPTION.equals(key)) {
                    payload.setException((Throwable) jsonReader.read(getThrowableType(typeName)));
                }
            }
            return payload;
        } catch (ClassNotFoundException e) {
            throw new SerializerException(e.getMessage());
        }
    }

    /**
     * 解析应答
     *
     * @param jsonReader 读取器
     * @param typeName   名称
     * @return 应答对象
     */
    protected Object parseResponse(final JSONReader jsonReader, final String typeName) {
        if (typeName == null || typeName.isEmpty()) {
            return null;
        }
        try {
            return jsonReader.read(getType(typeName));
        } catch (ClassNotFoundException e) {
            //泛化调用情况下，类可能不存在
            //TODO 需要判断是泛化调用
            return jsonReader.readObject();
        }
    }

    /**
     * 获取异常类型
     *
     * @param typeName 类型名称
     * @return 异常类型
     * @throws ClassNotFoundException
     */
    protected Class<?> getThrowableType(final String typeName) throws ClassNotFoundException {
        Class<?> clazz = ClassUtils.getClass(typeName);
        if (clazz == null) {
            return Throwable.class;
        } else if (Throwable.class.isAssignableFrom(clazz)) {
            return clazz;
        } else {
            throw new SerializerException("syntax error: invalid throwable class " + typeName);
        }
    }

    /**
     * 根据类型名称获取类型
     *
     * @param typeName 类型名称
     * @return 类型
     * @throws ClassNotFoundException
     */
    protected Type getType(final String typeName) throws ClassNotFoundException {
        Type type = getReturnGenericType(typeName);
        type = type == null ? ClassUtils.getClass(typeName) : type;
        return type;
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
