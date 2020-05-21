package io.joyrpc.trace.skywalking;

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.Trace;
import io.joyrpc.trace.TraceFactory;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;

import java.util.Map;

/**
 * skywalking跟踪工厂
 */
@Extension(value = "skywalking", order = 100)
@ConditionalOnClass("org.apache.skywalking.apm.agent.core.context.ContextManager")
public class SkywalkingTraceFactory implements TraceFactory {

    @Override
    public Trace create(final RequestMessage<Invocation> request) {
        return request.isConsumer() ? new ConsumerTrace(request) : new ProviderTrace(request);
    }

    /**
     * 抽象的跟踪
     */
    protected static abstract class AbstracetTrace implements Trace {
        protected RequestMessage<Invocation> request;
        protected Invocation invocation;
        protected ContextSnapshot snapshot;
        protected AbstractSpan span;

        public AbstracetTrace(RequestMessage<Invocation> request) {
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

        @Override
        public void onException(final Throwable throwable) {
            AbstractSpan activeSpan = ContextManager.activeSpan();
            activeSpan.errorOccurred();
            activeSpan.log(throwable);
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
        public void end() {
            ContextManager.stopSpan();
        }
    }

    /**
     * 消费者跟踪
     */
    protected static class ConsumerTrace extends AbstracetTrace {

        public ConsumerTrace(RequestMessage<Invocation> request) {
            super(request);
        }

        @Override
        public void begin(final String name, final String component, final Map<String, String> tags) {
            final ContextCarrier contextCarrier = new ContextCarrier();
            span = ContextManager.createExitSpan(name, contextCarrier, null);
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                invocation.addAttachment(next.getHeadKey(), next.getHeadValue());
            }
            span.setComponent(component);
            tag(tags);
            SpanLayer.asRPCFramework(span);
        }
    }

    /**
     * 生产者跟踪
     */
    protected static class ProviderTrace extends AbstracetTrace {

        public ProviderTrace(RequestMessage<Invocation> request) {
            super(request);
        }

        @Override
        public void begin(final String name, final String component, final Map<String, String> tags) {
            ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                next.setHeadValue(invocation.getAttachment(next.getHeadKey()));
            }

            span = ContextManager.createEntrySpan(name, contextCarrier);
            span.setComponent(component);
            tag(tags);
            SpanLayer.asRPCFramework(span);
        }
    }
}
