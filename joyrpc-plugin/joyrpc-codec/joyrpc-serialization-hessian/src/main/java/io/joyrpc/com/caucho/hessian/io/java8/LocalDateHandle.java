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

import java.time.LocalDate;

/**
 * LocalDate包装器
 */
public class LocalDateHandle implements Java8TimeWrapper<LocalDate> {

    private static final long serialVersionUID = 8739388063645761985L;
    protected int year;
    protected int month;
    protected int day;

    public LocalDateHandle() {
    }

    public LocalDateHandle(LocalDate localDate) {
        wrap(localDate);
    }

    @Override
    public void wrap(final LocalDate localDate) {
        if (localDate == null) {
        }
        this.year = localDate.getYear();
        this.month = localDate.getMonthValue();
        this.day = localDate.getDayOfMonth();
    }

    @Override
    public LocalDate readResolve() {
        return LocalDate.of(year, month, day);
    }
}
