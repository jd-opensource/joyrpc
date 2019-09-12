package io.joyrpc.com.caucho.hessian.io.java8;

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

import io.joyrpc.com.caucho.hessian.io.HessianHandle;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * ZonedDateTime 包装器
 */
public class ZonedDateTimeHandle implements Java8TimeWrapper<ZonedDateTime>, HessianHandle {

    private static final long serialVersionUID = -5652272593267656620L;

    protected LocalDateTime dateTime;
    protected ZoneOffset offset;
    protected String zoneId;

    public ZonedDateTimeHandle() {
    }

    public ZonedDateTimeHandle(ZonedDateTime zonedDateTime) {
        wrap(zonedDateTime);
    }

    @Override
    public void wrap(final ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return;
        }
        this.dateTime = zonedDateTime.toLocalDateTime();
        this.offset = zonedDateTime.getOffset();
        if (zonedDateTime.getZone() != null) {
            this.zoneId = zonedDateTime.getZone().getId();
        }
    }

    @Override
    public ZonedDateTime readResolve() {
        return ZonedDateTime.ofLocal(dateTime, ZoneId.of(zoneId), offset);
    }
}
