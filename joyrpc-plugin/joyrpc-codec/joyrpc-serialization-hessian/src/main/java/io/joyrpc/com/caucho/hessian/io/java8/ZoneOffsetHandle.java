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

import java.time.ZoneOffset;

/**
 * ZoneOffset包装器
 */
public class ZoneOffsetHandle implements Java8TimeWrapper<ZoneOffset>, HessianHandle {

    private static final long serialVersionUID = 867692662489392475L;
    private int seconds;

    public ZoneOffsetHandle() {
    }

    public ZoneOffsetHandle(ZoneOffset zoneOffset) {
        wrap(zoneOffset);
    }

    @Override
    public void wrap(final ZoneOffset zoneOffset) {
        if (zoneOffset == null) {
            return;
        }
        this.seconds = zoneOffset.getTotalSeconds();
    }

    @Override
    public ZoneOffset readResolve() {
        return ZoneOffset.ofTotalSeconds(seconds);
    }
}
