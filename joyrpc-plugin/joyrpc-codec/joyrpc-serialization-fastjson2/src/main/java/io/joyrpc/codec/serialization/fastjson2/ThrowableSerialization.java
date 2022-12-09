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
import com.alibaba.fastjson2.writer.ObjectWriter;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.util.ClassUtils;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Throwable序列化
 */
public class ThrowableSerialization extends AbstractSerialization<Throwable> {

    public static final ThrowableSerialization INSTANCE = new ThrowableSerialization();

    @Override
    protected void doWrite(final JSONWriter jsonWriter, final Throwable object, final Object fieldName, final Type fieldType, final long features) {
        writeObjectBegin(jsonWriter);
        writeString(jsonWriter, Constants.ANNOTATION_TYPE, object.getClass().getName(), false);
        writeString(jsonWriter, Constants.FIELD_MESSAGE, object.getMessage(), false);
        if (object.getCause() != object) {
            writeObject(jsonWriter, Constants.FIELD_CAUSE, object.getCause(), true);
        }
        StackTraceElement[] traces = object.getStackTrace();
        if (traces != null) {
            jsonWriter.writeName(Constants.FIELD_STACKTRACE);
            jsonWriter.writeColon();
            jsonWriter.startArray();
            ObjectWriter objectWriter = jsonWriter.getObjectWriter(StackTraceElement.class);
            for (int i = 0; i < traces.length; i++) {
                if (i > 0) {
                    jsonWriter.writeComma();
                }
                objectWriter.write(jsonWriter, traces[i]);
            }
            jsonWriter.endArray();
        }
        writeObjectEnd(jsonWriter);
    }

    @Override
    protected Throwable doRead(final JSONReader jsonReader, final Type fieldType, final Object fieldName, final long features) {
        String message = null;
        Throwable cause = null;
        Class<?> clazz = null;
        List<StackTraceElement> stackTraces = null;
        jsonReader.nextIfObjectStart();
        while (!jsonReader.nextIfObjectEnd() && !jsonReader.isEnd()) {
            switch (jsonReader.readFieldName()) {
                case Constants.ANNOTATION_TYPE:
                    String className = jsonReader.readString();
                    if (className != null) {
                        try {
                            clazz = ClassUtils.forName(className);
                        } catch (ClassNotFoundException e) {
                            throw new SerializerException("error occurs while parsing " + fieldName, e);
                        }
                    }
                    break;
                case Constants.FIELD_MESSAGE:
                    message = jsonReader.readString();
                    break;
                case Constants.FIELD_CAUSE:
                    cause = jsonReader.read(Throwable.class);
                    break;
                case Constants.FIELD_STACKTRACE:
                    stackTraces = jsonReader.readArray(StackTraceElement.class);
                    break;
            }
        }
        return ClassUtils.createException(clazz, message, cause,
                stackTraces == null ? null : stackTraces.toArray(new StackTraceElement[stackTraces.size()]));
    }
}
