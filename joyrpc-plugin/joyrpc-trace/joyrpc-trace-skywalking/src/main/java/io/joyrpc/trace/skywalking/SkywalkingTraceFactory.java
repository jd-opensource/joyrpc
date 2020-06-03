package io.joyrpc.trace.skywalking;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.TraceFactory;
import io.joyrpc.trace.Tracer;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;

import java.util.HashMap;
import java.util.Map;

/**
 * skywalking跟踪工厂
 */
@Extension(value = "skywalking", order = TraceFactory.ORDER_SKYWALKING)
@ConditionalOnClass("org.apache.skywalking.apm.agent.core.context.ContextManager")
public class SkywalkingTraceFactory implements TraceFactory {

    /**
     * 隐藏属性的key：分布式跟踪 数据KEY
     */
    public static final String HIDDEN_KEY_TRACE_SKYWALKING = ".skywalking";

    @Override
    public Tracer create(final RequestMessage<Invocation> request) {
        return request.isConsumer() ? new ConsumerTracer(request) : new ProviderTracer(request);
    }

    /**
     * 抽象的跟踪
     */
    protected static abstract class AbstractTracer implements Tracer {
        protected RequestMessage<Invocation> request;
        protected Invocation invocation;
        protected ContextSnapshot snapshot;
        protected AbstractSpan span;

        public AbstractTracer(RequestMessage<Invocation> request) {
            this.request = request;
            this.invocation = request.getPayLoad();
        }

        @Override
        public void snapshot() {
            snapshot = ContextManager.capture();
        }

        @Override
        public void restore() {
            ContextManager.continued(snapshot);
        }

        /**
         * 打标签
         *
         * @param tags 标签
         */
        protected void tag(final Map<String, String> tags) {
            if (tags != null) {
                tags.forEach((key, value) -> {
                    StringTag tag = new StringTag(key);
                    tag.set(span, value);
                });
            }
        }

        @Override
        public void end(final Throwable throwable) {
            if (throwable != null) {
                AbstractSpan activeSpan = ContextManager.activeSpan();
                activeSpan.errorOccurred();
                activeSpan.log(throwable);
            }
            ContextManager.stopSpan();
        }
    }

    /**
     * 消费者跟踪
     */
    protected static class ConsumerTracer extends AbstractTracer {

        public ConsumerTracer(RequestMessage<Invocation> request) {
            super(request);
        }

        @Override
        public void begin(final String name, final String component, final Map<String, String> tags) {
            Map<String, String> ctx = new HashMap<>();
            ContextCarrier contextCarrier = new ContextCarrier();
            span = ContextManager.createExitSpan(name, contextCarrier, null);
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                ctx.put(next.getHeadKey(), next.getHeadValue());
            }
            invocation.addAttachment(HIDDEN_KEY_TRACE_SKYWALKING, ctx);
            span.setComponent(component);
            tag(tags);
            SpanLayer.asRPCFramework(span);
        }
    }

    /**
     * 生产者跟踪
     */
    protected static class ProviderTracer extends AbstractTracer {

        public ProviderTracer(RequestMessage<Invocation> request) {
            super(request);
        }

        @Override
        public void begin(final String name, final String component, final Map<String, String> tags) {
            Map<String, String> ctx = (Map<String, String>) invocation.removeAttachment(HIDDEN_KEY_TRACE_SKYWALKING);
            ContextCarrier contextCarrier = new ContextCarrier();
            if (ctx != null) {
                CarrierItem next = contextCarrier.items();
                while (next.hasNext()) {
                    next = next.next();
                    next.setHeadValue(ctx.get(next.getHeadKey()));
                }
            }
            span = ContextManager.createEntrySpan(name, contextCarrier);
            span.setComponent(component);
            tag(tags);
            SpanLayer.asRPCFramework(span);
        }
    }
}
