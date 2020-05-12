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
import java.time.MonthDay;
import java.util.HashMap;
import java.util.Map;

public class MonthDaySchema extends AbstractJava8Schema<MonthDay> {

    public static final MonthDaySchema INSTANCE = new MonthDaySchema();
    public static final String MONTH = "month";
    public static final String DAY = "day";

    protected static final Map<String, Integer> FIELD_MAP = new HashMap(2);

    protected static Field FIELD_MONTH = getWriteableField(MonthDay.class, MONTH);
    protected static Field FIELD_DAY = getWriteableField(MonthDay.class, DAY);

    static {
        FIELD_MAP.put(MONTH, 1);
        FIELD_MAP.put(DAY, 2);
    }


    public MonthDaySchema() {
        super(MonthDay.class);
    }

    @Override
    public String getFieldName(int number) {
        switch (number) {
            case 1:
                return MONTH;
            case 2:
                return DAY;
            default:
                return null;
        }
    }

    @Override
    public int getFieldNumber(final String name) {
        return FIELD_MAP.get(name);
    }

    @Override
    public MonthDay newMessage() {
        return MonthDay.of(1, 1);
    }

    @Override
    public void mergeFrom(final Input input, final MonthDay message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    setValue(FIELD_MONTH, message, input.readInt32());
                    break;
                case 2:
                    setValue(FIELD_DAY, message, input.readInt32());
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final MonthDay message) throws IOException {
        output.writeInt32(1, message.getMonthValue(), false);
        output.writeInt32(2, message.getDayOfMonth(), false);
    }
}
