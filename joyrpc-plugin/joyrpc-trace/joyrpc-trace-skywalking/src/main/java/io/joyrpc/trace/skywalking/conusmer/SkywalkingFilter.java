package io.joyrpc.trace.skywalking.conusmer;

import io.joyrpc.config.InterfaceOption;
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.skywalking.AbstractSkywalkingFilter;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;

/**
 * 消费者APM过滤器
 */
@Extension(value = "skywalking", order = ConsumerFilter.TRACE_ORDER)
@ConditionalOnClass("org.apache.skywalking.apm.agent.core.context.ContextManager")
public class SkywalkingFilter extends AbstractSkywalkingFilter implements ConsumerFilter {

    @Override
    protected void beginTrace(final RequestMessage<Invocation> request) {
        InterfaceOption.MethodOption option = request.getOption();
        Invocation invocation = request.getPayLoad();

        final ContextCarrier contextCarrier = new ContextCarrier();
        AbstractSpan span = ContextManager.createExitSpan(option.getTraceSpanId(invocation), contextCarrier, null);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            invocation.addAttachment(next.getHeadKey(), next.getHeadValue());
        }
        //Tags.URL.set(span, generateRequestURL(requestURL, invocation));
        CONSUMER_ALIAS_TAG.set(span, invocation.getAlias());
        CONSUMER_NAME_TAG.set(span, invocation.getAttachment(Constants.HIDDEN_KEY_APPNAME));
        span.setComponent(component);
        SpanLayer.asRPCFramework(span);
    }

}
