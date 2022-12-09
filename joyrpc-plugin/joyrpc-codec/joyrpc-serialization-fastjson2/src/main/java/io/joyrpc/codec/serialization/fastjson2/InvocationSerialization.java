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
import io.joyrpc.exception.MethodOverloadException;
import io.joyrpc.exception.SerializerException;
import io.joyrpc.protocol.message.Invocation;

import java.lang.reflect.Type;

import static io.joyrpc.protocol.message.Invocation.*;

/**
 * Title: Invocation json 序列化<br>
 * <p/>
 * Description: <br>
 * 保证序列化字段按如下顺序：<br>
 * 1、class name 即接口名称<br>
 * 2、alias<br>
 * 3、method name<br>
 * 4、argsType callback 调用才会写
 * 5、args 参数value<br>
 * 6、attachments (值不为空则序列化)<br>
 * <p/>
 */
public class InvocationSerialization extends AbstractSerialization<Invocation> {

    public static final InvocationSerialization INSTANCE = new InvocationSerialization();

    @Override
    protected void doWrite(final JSONWriter jsonWriter, final Invocation object, final Object fieldName, final Type fieldType, final long features) {
        writeObjectBegin(jsonWriter);
        //1、class name
        writeString(jsonWriter, CLASS_NAME, object.getClassName(), true);
        //2、alias
        writeString(jsonWriter, ALIAS, object.getAlias(), true);
        //3、method name
        writeString(jsonWriter, METHOD_NAME, object.getMethodName(), true);
        //4.argsType
        //TODO 应该根据泛型变量来决定是否要参数类型
        if (object.isCallback()) {
            //回调需要写上实际的参数类型
            writeStringArray(jsonWriter, ARGS_TYPE, object.computeArgsType(), false);
        }
        //5、args
        writeObjectArray(jsonWriter, ARGS, object.getArgs(), true);
        //7、attachments
        writeObject(jsonWriter, ATTACHMENTS, object.getAttachments(), false);
        writeObjectEnd(jsonWriter);
    }

    @Override
    public Invocation doRead(final JSONReader jsonReader, final Type fieldType, final Object fieldName, final long features) {

        Invocation invocation = new Invocation();
        String key;
        jsonReader.nextIfObjectStart();
        while (!jsonReader.nextIfObjectEnd() && !jsonReader.isEnd()) {
            key = jsonReader.readFieldName();
            switch (key) {
                case CLASS_NAME -> invocation.setClassName(readString(jsonReader, CLASS_NAME, false));
                case ALIAS -> invocation.setAlias(readString(jsonReader, ALIAS, true));
                case METHOD_NAME -> invocation.setMethodName(readString(jsonReader, METHOD_NAME, false));
                case ARGS_TYPE -> invocation.setArgsType(readStringArray(jsonReader, ARGS_TYPE, true));
                case ARGS -> {
                    try {
                        invocation.setArgs(readObjectArray(jsonReader, ARGS, invocation.computeTypes()));
                    } catch (ClassNotFoundException e) {
                        throw new SerializerException("error occurs while parsing " + fieldName, e);
                    } catch (NoSuchMethodException e) {
                        throw new SerializerException("error occurs while parsing " + fieldName, e);
                    } catch (MethodOverloadException e) {
                        throw new SerializerException("error occurs while parsing " + fieldName, e);
                    }
                }
                case ATTACHMENTS -> invocation.addAttachments(readObject(jsonReader, ATTACHMENTS, true));

            }
        }
        return invocation;
    }

}
