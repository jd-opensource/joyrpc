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

import java.time.MonthDay;

/**
 * MonthDay包装器
 */
public class MonthDayHandle implements Java8TimeWrapper<MonthDay>, HessianHandle {
    private static final long serialVersionUID = 1842458305984530303L;
    protected int month;
    protected int day;

    public MonthDayHandle() {
    }

    public MonthDayHandle(MonthDay monthDay) {
        wrap(monthDay);
    }

    @Override
    public void wrap(final MonthDay monthDay) {
        if (monthDay == null) {
            return;
        }
        this.month = monthDay.getMonthValue();
        this.day = monthDay.getDayOfMonth();
    }

    @Override
    public MonthDay readResolve() {
        return MonthDay.of(month, day);
    }
}
