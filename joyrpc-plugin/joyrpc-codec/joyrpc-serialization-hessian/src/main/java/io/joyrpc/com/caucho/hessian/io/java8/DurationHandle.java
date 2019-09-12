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

import java.time.Duration;

/**
 * Duration包装器
 */
public class DurationHandle implements Java8TimeWrapper<Duration>, HessianHandle {

    private static final long serialVersionUID = 8897104281275173430L;

    protected long seconds;
    protected int nano;

    public DurationHandle() {
    }

    public DurationHandle(Duration duration) {
        wrap(duration);
    }

    @Override
    public void wrap(final Duration duration) {
        if (duration == null) {
            return;
        }
        this.seconds = duration.getSeconds();
        this.nano = duration.getNano();
    }

    @Override
    public Duration readResolve() {
        return Duration.ofSeconds(seconds, nano);
    }
}
