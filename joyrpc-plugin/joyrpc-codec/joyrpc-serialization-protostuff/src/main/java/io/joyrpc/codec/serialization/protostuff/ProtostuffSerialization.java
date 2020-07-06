package io.joyrpc.codec.serialization.protostuff;

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

import io.joyrpc.codec.serialization.*;
import io.joyrpc.codec.serialization.protostuff.schema.*;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.protostuff.*;
import io.protostuff.runtime.DefaultIdStrategy;
import io.protostuff.runtime.IdStrategy;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.util.Locale;

/**
 * Protostuff
 */
@Extension(value = "protostuff", provider = "protostuff", order = Serialization.ORDER_PROTOSTUFF)
@ConditionalOnClass("io.protostuff.runtime.RuntimeSchema")
public class ProtostuffSerialization implements Serialization {

    @Override
    public byte getTypeId() {
        return PROTOSTUFF_ID;
    }

    @Override
    public String getContentType() {
        return "application/x-protostuff";
    }

    @Override
    public Serializer getSerializer() {
        return ProtostuffSerializer.INSTANCE;
    }

    /**
     * Protostuff序列化和反序列化实现
     */
    protected static class ProtostuffSerializer extends AbstractSerializer {

        protected static final ProtostuffSerializer INSTANCE = new ProtostuffSerializer();

        protected static final DefaultIdStrategy STRATEGY = new DefaultIdStrategy(IdStrategy.DEFAULT_FLAGS |
                IdStrategy.ALLOW_NULL_ARRAY_ELEMENT);

        static {
            STRATEGY.registerPojo(Duration.class, DurationSchema.INSTANCE);
            STRATEGY.registerPojo(Instant.class, InstantSchema.INSTANCE);
            STRATEGY.registerPojo(LocalDate.class, LocalDateSchema.INSTANCE);
            STRATEGY.registerPojo(LocalTime.class, LocalTimeSchema.INSTANCE);
            STRATEGY.registerPojo(LocalDateTime.class, LocalDateTimeSchema.INSTANCE);
            STRATEGY.registerPojo(MonthDay.class, MonthDaySchema.INSTANCE);
            STRATEGY.registerPojo(OffsetDateTime.class, OffsetDateTimeSchema.INSTANCE);
            STRATEGY.registerPojo(OffsetTime.class, OffsetTimeSchema.INSTANCE);
            STRATEGY.registerPojo(Period.class, PeriodSchema.INSTANCE);
            STRATEGY.registerPojo(YearMonth.class, YearMonthSchema.INSTANCE);
            STRATEGY.registerPojo(Year.class, YearSchema.INSTANCE);
            STRATEGY.registerPojo(ZoneId.class, ZoneIdSchema.INSTANCE);
            STRATEGY.registerPojo(ZoneOffset.class, ZoneOffsetSchema.INSTANCE);
            STRATEGY.registerPojo(ZonedDateTime.class, ZonedDateTimeSchema.INSTANCE);
            STRATEGY.registerPojo(Date.class, SqlDateSchema.INSTANCE);
            STRATEGY.registerPojo(Time.class, SqlTimeSchema.INSTANCE);
            STRATEGY.registerPojo(Timestamp.class, SqlTimestampSchema.INSTANCE);
            STRATEGY.registerPojo(Locale.class, LocaleSchema.INSTANCE);
            //ID_STRATEGY.ARRAY_SCHEMA
            //注册插件，便于第三方协议注册序列化实现
            register(AutowiredObjectSerializer.class, o -> STRATEGY.registerPojo(o.getType(), o));
        }

        protected ThreadLocal<LinkedBuffer> local = ThreadLocal.withInitial(() -> LinkedBuffer.allocate(1024));

        protected ProtostuffSerializer() {
        }

        @Override
        protected ObjectWriter createWriter(final OutputStream os, final Object object) throws IOException {
            LinkedBuffer buffer = local.get();
            return new ProtostuffWriter(RuntimeSchema.getSchema(object.getClass(), STRATEGY), new ProtostuffOutput(buffer, os), os);
        }

        @Override
        protected ObjectReader createReader(final InputStream is, final Class clazz) throws IOException {
            return new ProtostuffReader(RuntimeSchema.getSchema(clazz, STRATEGY), is, local.get());
        }
    }

}
