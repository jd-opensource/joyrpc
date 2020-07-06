package io.joyrpc.protocol.grpc;

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

import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.joyrpc.transport.http2.DefaultHttp2Headers;
import io.joyrpc.transport.http2.Http2Headers;

import static io.joyrpc.constants.Constants.GRPC_MESSAGE_KEY;
import static io.joyrpc.constants.Constants.GRPC_STATUS_KEY;

/**
 * 头构建器
 */
public abstract class Headers {

    /**
     * 设置结束头
     *
     * @param end 是否结束
     * @return 头
     */
    public static Http2Headers build(final boolean end) {
        Http2Headers headers = new DefaultHttp2Headers();
        headers.status("200");
        headers.set(GrpcUtil.CONTENT_TYPE_KEY.name(), GrpcUtil.CONTENT_TYPE_GRPC);
        if (end) {
            headers.set(GRPC_STATUS_KEY, Status.Code.OK.value());
        }
        return headers;
    }

    /**
     * 设置异常结束头
     *
     * @param throwable 异常
     * @return 头
     */
    public static Http2Headers build(final Throwable throwable) {
        String errorMsg = throwable.getClass().getName() + ":" + throwable.getMessage();
        Http2Headers headers = new DefaultHttp2Headers();
        headers.status("200");
        headers.set(GrpcUtil.CONTENT_TYPE_KEY.name(), GrpcUtil.CONTENT_TYPE_GRPC);
        headers.set(GRPC_STATUS_KEY, Status.Code.INTERNAL.value());
        headers.set(GRPC_MESSAGE_KEY, errorMsg);
        return headers;
    }
}
