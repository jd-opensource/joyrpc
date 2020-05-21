package io.joyrpc.trace;

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

import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

/**
 * 跟踪工厂
 */
public interface TraceFactory {

    int ORDER_SKYWALKING = 100;
    int ORDER_PINPOINT = ORDER_SKYWALKING + 1;
    int ORDER_JAEGER = ORDER_PINPOINT + 1;
    int ORDER_ZIPKIN = ORDER_JAEGER + 1;

    /**
     * 构造跟踪会话
     *
     * @param request
     * @return
     */
    Tracer create(RequestMessage<Invocation> request);

}
