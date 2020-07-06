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

import java.time.Year;
import java.time.temporal.ChronoField;

/**
 * Year包装器
 */
public class YearHandle implements Java8TimeWrapper<Year> {

    private static final long serialVersionUID = -2817915438221390743L;
    protected int year;

    public YearHandle() {
    }

    public YearHandle(Year year) {
        wrap(year);
    }

    @Override
    public void wrap(final Year time) {
        this.year = time.getValue();
    }

    @Override
    public Year readResolve() {
        ChronoField.YEAR.checkValidValue(year);
        return Year.of(year);
    }
}
