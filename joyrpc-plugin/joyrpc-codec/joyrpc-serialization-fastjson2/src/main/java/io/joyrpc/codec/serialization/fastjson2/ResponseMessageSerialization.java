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
import com.alibaba.fastjson2.reader.ObjectReader;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.protocol.message.ResponsePayload;

import java.lang.reflect.Type;

/**
 * 应答序列化
 */
public class ResponseMessageSerialization extends AbstractSerialization<ResponseMessage<ResponsePayload>> {

    public static final ResponseMessageSerialization INSTANCE = new ResponseMessageSerialization();

    @Override
    protected void doWrite(final JSONWriter jsonWriter, final ResponseMessage<ResponsePayload> object, final Object fieldName, final Type fieldType, final long features) {
        jsonWriter.getObjectWriter(ResponsePayload.class).write(jsonWriter, object.getPayLoad());
    }

    @Override
    protected ResponseMessage<ResponsePayload> doRead(final JSONReader jsonReader, final Type fieldType, final Object fieldName, final long features) {
        ResponseMessage<ResponsePayload> result = new ResponseMessage<>();
        ObjectReader<ResponsePayload> objectReader = jsonReader.getObjectReader(ResponsePayload.class);
        result.setPayLoad(objectReader.readObject(jsonReader));
        return null;
    }

}
