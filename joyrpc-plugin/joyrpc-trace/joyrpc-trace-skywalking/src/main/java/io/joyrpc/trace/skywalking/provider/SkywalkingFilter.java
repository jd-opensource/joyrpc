package io.joyrpc.trace.skywalking.provider;

import io.joyrpc.config.InterfaceOption;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.filter.ProviderFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.trace.skywalking.AbstractSkywalkingFilter;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;

/**
 * 服务提供者APM过滤器
 */
@Extension(value = "skywalking", order = ProviderFilter.TRACE_ORDER)
@ConditionalOnClass("org.apache.skywalking.apm.agent.core.context.ContextManager")
public class SkywalkingFilter extends AbstractSkywalkingFilter implements ProviderFilter {

    @Override
    protected void beginTrace(RequestMessage<Invocation> request) {
        InterfaceOption.MethodOption option = request.getOption();
        Invocation invocation = request.getPayLoad();

        ContextCarrier contextCarrier = new ContextCarrier();
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            next.setHeadValue(invocation.getAttachment(next.getHeadKey()));
        }

        AbstractSpan span = ContextManager.createEntrySpan(option.getTraceSpanId(invocation), contextCarrier);
        ALIAS_TAG.set(span, invocation.getAlias());
        //Tags.URL.set(span, generateRequestURL(requestURL, invocation));
        span.setPeer(request.getRemoteAddress().toString());
        span.setComponent(component);
        SpanLayer.asRPCFramework(span);
    }
}
