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
import java.time.LocalDate;

public class LocalDateSchema extends AbstractJava8Schema<LocalDate> {

    public static final LocalDateSchema INSTANCE = new LocalDateSchema();
    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String DAY = "day";
    protected static Field FIELD_YEAR = getWriteableField(LocalDate.class, YEAR);
    protected static Field FIELD_MONTH = getWriteableField(LocalDate.class, MONTH);
    protected static Field FIELD_DAY = getWriteableField(LocalDate.class, DAY);

    public LocalDateSchema() {
        super(LocalDate.class);
    }

    @Override
    public String getFieldName(int number) {
        return switch (number) {
            case 1 -> YEAR;
            case 2 -> MONTH;
            case 3 -> DAY;
            default -> null;
        };
    }

    @Override
    public int getFieldNumber(final String name) {
        return switch (name) {
            case YEAR -> 1;
            case MONTH -> 2;
            case DAY -> 3;
            default -> 0;
        };
    }

    @Override
    public LocalDate newMessage() {
        return LocalDate.now();
    }

    @Override
    public void mergeFrom(final Input input, final LocalDate message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    setValue(FIELD_YEAR, message, input.readInt32());
                    break;
                case 2:
                    setValue(FIELD_MONTH, message, (short) input.readInt32());
                    break;
                case 3:
                    setValue(FIELD_DAY, message, (short) input.readInt32());
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final LocalDate message) throws IOException {
        output.writeInt32(1, message.getYear(), false);
        output.writeInt32(2, message.getMonthValue(), false);
        output.writeInt32(3, message.getDayOfMonth(), false);
    }
}
