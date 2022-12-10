/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.com.caucho.hessian.io.java8;

import io.joyrpc.com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.time.Duration;

public class DurationHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -4367309317780077156L;

    private long seconds;
    private int nanos;

    public DurationHandle() {
    }

    public DurationHandle(Object o) {
        Duration duration = (Duration) o;
        this.seconds = duration.getSeconds();
        this.nanos = duration.getNano();
    }

    protected Object readResolve() {
        return Duration.ofSeconds(seconds, nanos);
    }

    public static HessianHandle create(Object o) {
        return new DurationHandle(o);
    }
}
