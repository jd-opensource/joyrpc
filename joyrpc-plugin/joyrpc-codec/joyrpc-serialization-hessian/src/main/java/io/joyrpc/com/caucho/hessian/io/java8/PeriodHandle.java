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

import java.time.Period;

/**
 * Period包装器
 */
public class PeriodHandle implements Java8TimeWrapper<Period> {

    private static final long serialVersionUID = 3853269100626256850L;
    protected int years;
    protected int months;
    protected int days;

    public PeriodHandle() {
    }

    public PeriodHandle(Period period) {
        wrap(period);
    }

    @Override
    public void wrap(final Period period) {
        this.years = period.getYears();
        this.months = period.getMonths();
        this.days = period.getDays();
    }

    @Override
    public Period readResolve() {
        return Period.of(years, months, days);
    }
}
