package io.joyrpc.context.injection.trace;

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

import io.joyrpc.cluster.Node;
import io.joyrpc.context.injection.NodeReqInjection;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.Tracers;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

/**
 * 注入节点
 */
@Extension("opentracing")
public class SpanInjection implements NodeReqInjection {

    @Override
    public void inject(final RequestMessage<Invocation> request, final Node node) {
        Tracer tracer = Tracers.getTracer();
        if (tracer != null) {
            Span span = tracer.scopeManager().activeSpan();
            if (span != null) {
                URL url = node.getUrl();
                span.setTag(Tags.PEER_HOST_IPV4.getKey(), url.getHost());
                span.setTag(Tags.PEER_PORT.getKey(), String.valueOf(url.getPort()));
            }
        }
    }

    @Override
    public void reject(final RequestMessage<Invocation> request, final Node node) {
    }

    @Override
    public boolean test() {
        return true;
    }
}
