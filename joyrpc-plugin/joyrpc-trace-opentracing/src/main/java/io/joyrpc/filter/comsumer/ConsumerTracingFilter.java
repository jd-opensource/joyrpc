package io.joyrpc.filter.comsumer;

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
import io.joyrpc.constants.Constants;
import io.joyrpc.constants.Version;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.filter.AbstractTracingFilter;
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * The type Consumer tracing filter.
 *
 * @date: 1 /21/2019
 */
@Extension(value = "trace", order = ConsumerFilter.TRACE_ORDER)
public class ConsumerTracingFilter extends AbstractTracingFilter implements ConsumerFilter {

    @Override
    public Span tracing(final Tracer tracer, final Invoker invoker, final RequestMessage<Invocation> request) {
        String name = name(request);
        Invocation invocation = request.getPayLoad();
        Tracer.SpanBuilder builder = tracer.buildSpan(name)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                .withTag(Tags.COMPONENT.getKey(), GlobalContext.getString(Constants.PROTOCOL_VERSION_KEY, Version.PROTOCOL_VERSION))
                .withTag(Tags.PEER_SERVICE.getKey(), invocation.getClassName())
                .withTag(REQUEST_ID, request.getMsgId());
        //会自动成为当前活动Span的子节点
        Span span = builder.start();
        tracer.inject(span.context(), Format.Builtin.TEXT_MAP, new TraceTextMap(request));
        return span;
    }

}
