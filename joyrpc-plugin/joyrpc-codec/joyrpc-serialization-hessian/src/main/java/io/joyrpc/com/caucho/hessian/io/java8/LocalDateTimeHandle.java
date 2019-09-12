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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * LocalDateTime包装器
 */
public class LocalDateTimeHandle implements Java8TimeWrapper<LocalDateTime>, HessianHandle {

    private static final long serialVersionUID = 5535403026167970317L;
    protected LocalDate date;
    protected LocalTime time;

    public LocalDateTimeHandle() {
    }

    public LocalDateTimeHandle(LocalDateTime localDateTime) {
        wrap(localDateTime);
    }

    @Override
    public void wrap(final LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return;
        }
        this.date = localDateTime.toLocalDate();
        this.time = localDateTime.toLocalTime();
    }

    @Override
    public LocalDateTime readResolve() {
        return LocalDateTime.of(date, time);
    }
}
