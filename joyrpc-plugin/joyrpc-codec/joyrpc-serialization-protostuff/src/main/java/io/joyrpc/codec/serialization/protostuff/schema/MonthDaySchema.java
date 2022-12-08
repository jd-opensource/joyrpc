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

public class MonthDaySchema extends AbstractJava8Schema<MonthDay> {

    public static final MonthDaySchema INSTANCE = new MonthDaySchema();
    public static final String MONTH = "month";
    public static final String DAY = "day";
    protected static Field FIELD_MONTH = getWriteableField(MonthDay.class, MONTH);
    protected static Field FIELD_DAY = getWriteableField(MonthDay.class, DAY);

    public MonthDaySchema() {
        super(MonthDay.class);
    }

    @Override
    public String getFieldName(int number) {
        return switch (number) {
            case 1 -> MONTH;
            case 2 -> DAY;
            default -> null;
        };
    }

    @Override
    public int getFieldNumber(final String name) {
        return switch (name) {
            case MONTH -> 1;
            case DAY -> 2;
            default -> 0;
        };
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
