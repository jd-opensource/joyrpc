package io.joyrpc.filter.provider;

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
import io.joyrpc.context.RequestContext;
import io.joyrpc.extension.Extension;
import io.joyrpc.filter.AbstractTracingFilter;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.network.Ipv4;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;

/**
 * The type Provider tracing filter.
 *
 * @date: 1 /21/2019
 */
@Extension(value = "trace", order = ProviderFilter.TRACE_ORDER)
public class ProviderTracingFilter extends AbstractTracingFilter implements ProviderFilter {

    @Override
    public Span tracing(final Tracer tracer, final Invoker invoker, final RequestMessage<Invocation> request) {
        RequestContext context = RequestContext.getContext();
        String name = name(request);
        Invocation invocation = request.getPayLoad();
        Tracer.SpanBuilder builder = tracer.buildSpan(name)
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER)
                .withTag(Tags.COMPONENT.getKey(), "joy")
                .withTag(Tags.PEER_SERVICE.getKey(), invocation.getClassName())
                .withTag(Tags.PEER_HOST_IPV4.getKey(), Ipv4.toIp(context.getRemoteAddress()))
                .withTag(REQUEST_ID, request.getMsgId());
        SpanContext spanContext = tracer.extract(Format.Builtin.TEXT_MAP, new TraceTextMap(request));
        if (spanContext != null) {
            builder.asChildOf(spanContext);
        }
        Span span = builder.start();
        tracer.activateSpan(span);
        return span;
    }
}
