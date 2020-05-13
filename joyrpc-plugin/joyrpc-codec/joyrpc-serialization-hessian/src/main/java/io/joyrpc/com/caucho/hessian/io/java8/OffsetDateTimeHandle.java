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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * OffsetDateTime包装器
 */
public class OffsetDateTimeHandle implements Java8TimeWrapper<OffsetDateTime> {
    private static final long serialVersionUID = -4980501582624017719L;

    protected LocalDateTime dateTime;
    protected ZoneOffset offset;

    public OffsetDateTimeHandle() {
    }

    public OffsetDateTimeHandle(OffsetDateTime offsetDateTime) {
        wrap(offsetDateTime);
    }

    @Override
    public void wrap(final OffsetDateTime time) {
        this.dateTime = time.toLocalDateTime();
        this.offset = time.getOffset();
    }

    @Override
    public OffsetDateTime readResolve() {
        return OffsetDateTime.of(dateTime, offset);
    }
}
