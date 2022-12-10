package io.joyrpc.protocol.dubbo.serialization.hessian2;

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

import com.alibaba.com.caucho.hessian.io.java8.*;
import io.joyrpc.com.caucho.hessian.io.AbstractSerializerFactory;
import io.joyrpc.com.caucho.hessian.io.Deserializer;
import io.joyrpc.com.caucho.hessian.io.HessianProtocolException;
import io.joyrpc.com.caucho.hessian.io.Serializer;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义序列化器工厂类
 */
public class DubboHessian2SerializerFactory extends AbstractSerializerFactory {

    protected Map<Class<?>, Serializer> serializers = new HashMap<>();
    protected Map<Class<?>, Deserializer> deserializers = new HashMap<>();

    /**
     * 构造函数
     */
    public DubboHessian2SerializerFactory() {
        serializers.put(java.time.LocalTime.class, Java8TimeSerializer.create(LocalTimeHandle::create));
        serializers.put(java.time.LocalDate.class, Java8TimeSerializer.create(LocalDateHandle::create));
        serializers.put(java.time.LocalDateTime.class, Java8TimeSerializer.create(LocalDateTimeHandle::create));
        serializers.put(java.time.Instant.class, Java8TimeSerializer.create(InstantHandle::create));
        serializers.put(java.time.Duration.class, Java8TimeSerializer.create(DurationHandle::create));
        serializers.put(java.time.Period.class, Java8TimeSerializer.create(PeriodHandle::create));
        serializers.put(java.time.Year.class, Java8TimeSerializer.create(YearHandle::create));
        serializers.put(java.time.YearMonth.class, Java8TimeSerializer.create(YearMonthHandle::create));
        serializers.put(java.time.MonthDay.class, Java8TimeSerializer.create(MonthDayHandle::create));
        serializers.put(java.time.OffsetTime.class, Java8TimeSerializer.create(OffsetTimeHandle::create));
        serializers.put(java.time.ZoneOffset.class, Java8TimeSerializer.create(ZoneOffsetHandle::create));
        serializers.put(java.time.OffsetDateTime.class, Java8TimeSerializer.create(OffsetDateTimeHandle::create));
        serializers.put(java.time.ZonedDateTime.class, Java8TimeSerializer.create(ZonedDateTimeHandle::create));
        serializers.put(ZoneId.class, Java8TimeSerializer.create(ZoneIdHandle::create));
        serializers.put(ZoneId.systemDefault().getClass(), Java8TimeSerializer.create(ZoneIdHandle::create));
    }

    @Override
    public Serializer getSerializer(final Class cl) throws HessianProtocolException {
        return serializers.get(cl);
    }

    @Override
    public Deserializer getDeserializer(final Class cl) throws HessianProtocolException {
        return deserializers == null ? null : deserializers.get(cl);
    }
}
