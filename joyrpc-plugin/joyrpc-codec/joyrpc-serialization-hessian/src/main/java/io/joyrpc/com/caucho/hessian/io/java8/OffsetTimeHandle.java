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

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

/**
 *
 */
public class OffsetTimeHandle implements Java8TimeWrapper<OffsetTime>, HessianHandle {

    private static final long serialVersionUID = -1701639933686935988L;
    protected LocalTime localTime;
    protected ZoneOffset zoneOffset;

    public OffsetTimeHandle() {
    }

    public OffsetTimeHandle(OffsetTime offsetTime) {
        wrap(offsetTime);
    }

    @Override
    public void wrap(final OffsetTime offsetTime) {
        if (offsetTime != null) {
            this.zoneOffset = offsetTime.getOffset();
            this.localTime = offsetTime.toLocalTime();
        }
    }

    @Override
    public OffsetTime readResolve() {
        return OffsetTime.of(localTime, zoneOffset);
    }
}
