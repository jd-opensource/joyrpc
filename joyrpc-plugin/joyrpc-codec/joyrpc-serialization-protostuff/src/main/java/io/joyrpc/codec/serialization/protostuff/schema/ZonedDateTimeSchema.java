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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class ZonedDateTimeSchema extends AbstractJava8Schema<ZonedDateTime> {

    public static final ZonedDateTimeSchema INSTANCE = new ZonedDateTimeSchema();
    public static final String DATE_TIME = "dateTime";
    public static final String OFFSET = "offset";
    public static final String ZONE = "zone";

    protected static final Map<String, Integer> FIELD_MAP = new HashMap();

    protected static Field FIELD_DATE_TIME = getWriteableField(ZonedDateTime.class, DATE_TIME);
    protected static Field FIELD_ZONE_OFFSET = getWriteableField(ZonedDateTime.class, OFFSET);
    protected static Field FIELD_ZONE_ID = getWriteableField(ZonedDateTime.class, ZONE);

    static {
        FIELD_MAP.put(DATE_TIME, 1);
        FIELD_MAP.put(OFFSET, 2);
        FIELD_MAP.put(ZONE, 3);
    }

    public ZonedDateTimeSchema() {
        super(ZonedDateTime.class);
    }

    @Override
    public String getFieldName(int number) {
        switch (number) {
            case 1:
                return DATE_TIME;
            case 2:
                return OFFSET;
            case 3:
                return ZONE;
            default:
                return null;
        }
    }

    @Override
    public int getFieldNumber(final String name) {
        return FIELD_MAP.get(name);
    }

    @Override
    public ZonedDateTime newMessage() {
        return ZonedDateTime.now();
    }

    @Override
    public void mergeFrom(final Input input, final ZonedDateTime message) throws IOException {
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
                case 3:
                    ZoneId zoneId = ZoneId.of("America/New_York");
                    input.mergeObject(zoneId, ZoneIdSchema.INSTANCE);
                    setValue(FIELD_ZONE_ID, message, zoneId);
                    break;
                default:
                    input.handleUnknownField(number, this);
            }
        }
    }

    @Override
    public void writeTo(final Output output, final ZonedDateTime message) throws IOException {
        output.writeObject(1, message.toLocalDateTime(), LocalDateTimeSchema.INSTANCE, false);
        output.writeObject(2, message.getOffset(), ZoneOffsetSchema.INSTANCE, false);
        if (message.getZone() != null) {
            output.writeObject(3, message.getZone(), ZoneIdSchema.INSTANCE, false);
        }
    }
}
