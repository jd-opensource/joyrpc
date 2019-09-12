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

import java.time.YearMonth;

/**
 * YearMonth包装器
 */
public class YearMonthHandle implements Java8TimeWrapper<YearMonth>, HessianHandle {

    private static final long serialVersionUID = 4927217299568145798L;
    protected int year;
    protected int month;

    public YearMonthHandle() {
    }

    public YearMonthHandle(YearMonth yearMonth) {
        wrap(yearMonth);
    }

    @Override
    public void wrap(final YearMonth yearMonth) {
        if (yearMonth == null) {
            return;
        }
        this.year = yearMonth.getYear();
        this.month = yearMonth.getMonthValue();
    }

    @Override
    public YearMonth readResolve() {
        return YearMonth.of(year, month);
    }
}
