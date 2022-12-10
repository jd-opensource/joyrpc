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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class LocalDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = 7563825215275989361L;

    private Object date;
    private Object time;

    public LocalDateTimeHandle() {
    }

    public LocalDateTimeHandle(Object o) {
        LocalDateTime localDateTime = (LocalDateTime) o;
        this.date = localDateTime.toLocalDate();
        this.time = localDateTime.toLocalTime();
    }

    protected Object readResolve() {
        return LocalDateTime.of((LocalDate) date, (LocalTime) time);
    }

    public static HessianHandle create(Object o) {
        return new LocalDateTimeHandle(o);
    }
}
