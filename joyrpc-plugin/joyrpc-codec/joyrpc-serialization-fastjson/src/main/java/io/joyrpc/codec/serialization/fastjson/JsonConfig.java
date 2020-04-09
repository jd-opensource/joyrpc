package io.joyrpc.codec.serialization.fastjson;

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

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ASMDeserializerFactory;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.parser.deserializer.ThrowableDeserializer;
import io.joyrpc.permission.BlackList;

import java.lang.reflect.Type;

public class JsonConfig extends ParserConfig {

    protected BlackList<String> blackList;

    public JsonConfig(BlackList<String> blackList) {
        this.blackList = blackList;
    }

    public JsonConfig(boolean fieldBase, BlackList<String> blackList) {
        super(fieldBase);
        this.blackList = blackList;
    }

    public JsonConfig(ClassLoader parentClassLoader, BlackList<String> blackList) {
        super(parentClassLoader);
        this.blackList = blackList;
    }

    public JsonConfig(ASMDeserializerFactory asmFactory, BlackList<String> blackList) {
        super(asmFactory);
        this.blackList = blackList;
    }

    public ObjectDeserializer getDeserializer(final Class<?> clazz, final Type type) {

        if (blackList != null && blackList.isBlack(clazz.getName())) {
            throw new JSONException("Failed to decode class " + type + " by json serialization, it is in blacklist");
        }
        ObjectDeserializer deserializer = super.getDeserializer(clazz, type);
        if (deserializer instanceof ThrowableDeserializer) {
            //覆盖掉默认的异常解析器
            deserializer = new JsonThrowableDeserializer(this, clazz);
            //内部支持并发
            putDeserializer(type, deserializer);
        }
        return deserializer;
    }
}
