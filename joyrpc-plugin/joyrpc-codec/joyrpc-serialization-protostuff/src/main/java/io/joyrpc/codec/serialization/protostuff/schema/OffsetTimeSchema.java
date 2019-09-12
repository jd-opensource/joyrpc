package io.joyrpc.codec.serialization.protostuff.schema;

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

import io.protostuff.Input;
import io.protostuff.Output;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class OffsetTimeSchema extends AbstractJava8Schema<OffsetTime> {

    public static final OffsetTimeSchema INSTANCE = new OffsetTimeSchema();
    public static final String TIME = "time";
    public static final String OFFSET = "offset";

    protected static final String[] FIELD_NAMES = new String[]{TIME, OFFSET};

    protected static final Map<String, Integer> FIELD_MAP = new HashMap(2);

    protected static Field FIELD_TIME = getWriteableField(OffsetTime.class, TIME);
    protected static Field FIELD_OFFSET = getWriteableField(OffsetTime.class, OFFSET);

    static {
        FIELD_MAP.put(TIME, 1);
        FIELD_MAP.put(OFFSET, 2);
    }

    public OffsetTimeSchema() {
        super(OffsetTime.class);
    }

    @Override
    public String getFieldName(int number) {
        return FIELD_NAMES[number];
    }

    @Override
    public int getFieldNumber(final String name) {
        return FIELD_MAP.get(name);
    }

    @Override
    public OffsetTime newMessage() {
        return OffsetTime.now();
    }

    @Override
    public void mergeFrom(final Input input, final OffsetTime message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    LocalTime localTime = LocalTime.now();
                    input.mergeObject(localTime, LocalTimeSchema.INSTANCE);
                    setValue(FIELD_TIME, message, localTime);
                    break;
                case 2:
                    //不能使用0，0会缓存结果对象
                    ZoneOffset offset = ZoneOffset.ofTotalSeconds(1);
                    input.mergeObject(offset, ZoneOffsetSchema.INSTANCE);
                    setValue(FIELD_OFFSET, message, offset);
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final OffsetTime message) throws IOException {
        output.writeObject(1, message.toLocalTime(), LocalTimeSchema.INSTANCE, false);
        output.writeObject(2, message.getOffset(), ZoneOffsetSchema.INSTANCE, false);
    }
}
