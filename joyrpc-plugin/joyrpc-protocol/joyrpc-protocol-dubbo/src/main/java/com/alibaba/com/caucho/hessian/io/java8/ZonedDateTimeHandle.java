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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class ZonedDateTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -6933460123278647569L;

    private Object dateTime;
    private Object offset;
    private String zoneId;

    public ZonedDateTimeHandle() {
    }

    public ZonedDateTimeHandle(Object o) {
        ZonedDateTime zonedDateTime = (ZonedDateTime) o;
        this.dateTime = zonedDateTime.toLocalDateTime();
        this.offset = zonedDateTime.getOffset();
        this.zoneId = zonedDateTime.getZone().getId();
    }

    protected Object readResolve() {
        return ZonedDateTime.ofLocal((LocalDateTime) dateTime, ZoneId.of(zoneId), (ZoneOffset) offset);
    }

    public static HessianHandle create(Object o) {
        return new ZonedDateTimeHandle(o);
    }
}
