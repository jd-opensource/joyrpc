package io.joyrpc.filter;

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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.protocol.message.ResponseMessage;
import io.joyrpc.trace.Tracers;
import io.joyrpc.transport.message.Header;
import io.joyrpc.util.Futures;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracer;
import io.opentracing.propagation.TextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Tracing filter.
 *
 * @date: 1 /9/2019
 */
public abstract class AbstractTracingFilter extends AbstractFilter {

    /**
     * 前缀
     */
    public static final String RPC_TRACE = "rpc_trace";
    /**
     * The constant TRACE_SPAN_TAG_REQUEST_ID.
     */
    public static final String REQUEST_ID = "requestId";
    /**
     * The constant TRACE_SPAN_TAG_RESPONSE_SIZE.
     */
    public static final String RESPONSE_SIZE = "responseSize";
    /**
     * The constant TRACE_SPAN_TAG_RESULT_CODE.
     */
    public static final String RESULT_CODE = "result";
    /**
     * The constant RPC_RESULT_FAILED.
     */
    public static final String RPC_RESULT_FAILED = "01";
    /**
     * The constant RPC_RESULT_FAILED.
     */
    public static final String RPC_RESULT_SUCCESS = "00";

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        //先重全局获取
        Tracer tracer = Tracers.getTracer();
        if (tracer == null || tracer instanceof NoopTracer) {
            return invoker.invoke(request);
        }
        Span span = tracing(tracer, invoker, request);
        //scope会自动关闭，span必须在当前线程关闭
        try (Scope scope = tracer.activateSpan(span)) {
            return invoker.invoke(request).whenComplete((result, throwable) -> {
                try (Scope sc = tracer.activateSpan(span)) {
                    endSpan(span, throwable != null ? throwable :
                            (result != null && result.isException() ? result.getException() : null), result);
                }

            });
        } catch (Throwable e) {
            endSpan(span, e, null);
            return Futures.completeExceptionally(e);
        }

    }

    /**
     * Tracing response message.
     *
     * @param tracer  the tracer
     * @param request the request
     * @return the response message
     */
    protected abstract Span tracing(Tracer tracer, Invoker invoker, RequestMessage<Invocation> request);

    /**
     * 调用结束
     *
     * @param span
     * @param throwable
     * @param response
     */
    protected void endSpan(final Span span, final Throwable throwable, final Result response) {
        span.log(throwable == null ? "request success." : "request fail." + throwable.getMessage());
        int responseSize = 0;
        if (!response.isException() && null != response.getValue()) {
            ResponseMessage message = (ResponseMessage) response.getMessage();
            Header header = message == null ? null : message.getHeader();
            if (null != header && null != header.getLength()) {
                responseSize = header.getLength();
            }
        }
        //  span.setTag(TRACE_SPAN_TAG_SERVER_IP, RpcContext.getContext().getRemoteHostName());
        span.setTag(RESPONSE_SIZE, responseSize);
        span.setTag(RESULT_CODE, throwable != null ? RPC_RESULT_FAILED : RPC_RESULT_SUCCESS);
        span.finish();
    }

    /**
     * 操作名, 一个具有可读性的字符串，代表这个span所做的工作（例如：RPC方法名，方法名，或者一个大型计算中的某个阶段或子任务）。<br/>
     * 操作名应该是一个抽象、通用，明确、具有统计意义的名称
     *
     * @param request the request
     * @return the string
     */

    protected String name(final RequestMessage<Invocation> request) {
        return request.getPayLoad().getMethodName();
    }

    /**
     * TextMap实现
     */
    protected static class TraceTextMap implements TextMap {
        protected Map<String, String> attachments;

        public TraceTextMap(final RequestMessage<Invocation> request) {
            this.attachments = request.getPayLoad().computeIfAbsent(RPC_TRACE, o -> new HashMap<>());
        }

        @Override
        public void put(final String key, final String value) {
            attachments.put(key, value);
        }

        @Override
        public Iterator<Map.Entry<String, String>> iterator() {
            return attachments.entrySet().iterator();
        }
    }

}
