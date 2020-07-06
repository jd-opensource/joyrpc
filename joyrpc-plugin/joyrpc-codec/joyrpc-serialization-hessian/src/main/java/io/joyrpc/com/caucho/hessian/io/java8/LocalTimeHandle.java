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


import java.time.LocalTime;

/**
 * LocalTime包装器
 */
public class LocalTimeHandle implements Java8TimeWrapper<LocalTime> {

    private static final long serialVersionUID = -4914516760659633510L;
    protected int hour;
    protected int minute;
    protected int second;
    protected int nano;

    public LocalTimeHandle() {
    }

    public LocalTimeHandle(LocalTime localTime) {
        wrap(localTime);
    }

    @Override
    public void wrap(final LocalTime localTime) {
        this.hour = localTime.getHour();
        this.minute = localTime.getMinute();
        this.second = localTime.getSecond();
        this.nano = localTime.getNano();

    }

    @Override
    public LocalTime readResolve() {
        return LocalTime.of(hour, minute, second, nano);
    }
}
