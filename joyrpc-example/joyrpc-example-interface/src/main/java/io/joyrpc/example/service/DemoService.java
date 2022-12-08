package io.joyrpc.example.service;

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

import io.joyrpc.annotation.CallbackArg;
import io.joyrpc.example.service.vo.*;
import jakarta.validation.constraints.NotNull;

/**
 * Demo service
 */
public interface DemoService {

    String sayHello(@NotNull String str) throws Throwable;

    int test(int count);

    <T> T generic(T value);

    default String echo(String str) throws Throwable {
        return sayHello(str);
    }

    default Java8TimeObj echoJava8TimeObj(Java8TimeObj timeObj) {
        return timeObj;
    }

    default EchoResponse<EchoData> echoRequest(EchoRequest<EchoData> request) {
        return request == null ? null : new EchoResponse<>(request.getHeader(), request.getBody());
    }

    default EchoResponse echoRequestGeneric(EchoRequest request) {
        return request == null ? null : new EchoResponse<>(request.getHeader(), request.getBody());
    }

    default EchoResponse echoDataRequest(EchoDataRequest request) {
        return request == null ? null : new EchoResponse<>(request.getHeader(), request.getBody());
    }

    static String hello(String v) {
        return v;
    }

    void echoCallback(@CallbackArg EchoCallback callback);

    public static interface EchoCallback {

        boolean echo(String str);
    }
}
