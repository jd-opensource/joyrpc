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
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

public class OffsetDateTimeSchema extends AbstractJava8Schema<OffsetDateTime> {

    public static final OffsetDateTimeSchema INSTANCE = new OffsetDateTimeSchema();
    public static final String DATE_TIME = "dateTime";
    public static final String OFFSET = "offset";
    protected static Field FIELD_DATE_TIME = getWriteableField(OffsetDateTime.class, DATE_TIME);
    protected static Field FIELD_ZONE_OFFSET = getWriteableField(OffsetDateTime.class, OFFSET);

    public OffsetDateTimeSchema() {
        super(OffsetDateTime.class);
    }

    @Override
    public String getFieldName(int number) {
        return switch (number) {
            case 1 -> DATE_TIME;
            case 2 -> OFFSET;
            default -> null;
        };
    }

    @Override
    public int getFieldNumber(final String name) {
        return switch (name) {
            case DATE_TIME -> 1;
            case OFFSET -> 2;
            default -> 0;
        };
    }

    @Override
    public OffsetDateTime newMessage() {
        return OffsetDateTime.now();
    }

    @Override
    public void mergeFrom(final Input input, final OffsetDateTime message) throws IOException {
        while (true) {
            int number = input.readFieldNumber(this);
            switch (number) {
                case 0:
                    return;
                case 1:
                    LocalDateTime localDateTime = LocalDateTime.now();
                    input.mergeObject(localDateTime, LocalDateTimeSchema.INSTANCE);
                    setValue(FIELD_DATE_TIME, message, localDateTime);
                    break;
                case 2:
                    //不能使用0，0会缓存结果对象
                    ZoneOffset offset = ZoneOffset.ofTotalSeconds(1);
                    input.mergeObject(offset, ZoneOffsetSchema.INSTANCE);
                    setValue(FIELD_ZONE_OFFSET, message, offset);
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final OffsetDateTime message) throws IOException {
        output.writeObject(1, message.toLocalDateTime(), LocalDateTimeSchema.INSTANCE, false);
        output.writeObject(2, message.getOffset(), ZoneOffsetSchema.INSTANCE, false);
    }
}
