package io.joyrpc.trace.skywalking;

import io.joyrpc.config.InterfaceOption;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.context.RequestContext;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.OfficialComponent;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.PROTOCOL_KEY;

/**
 * 拦截器
 */
public class MyReferInterceptor implements InstanceMethodsAroundInterceptor {

    public static final int ID = 2000;

    protected String protocol;

    protected String getProtocol() {
        if (protocol == null) {
            protocol = GlobalContext.getString(PROTOCOL_KEY);
        }
        return protocol;
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        RequestMessage<Invocation> request = (RequestMessage<Invocation>) allArguments[0];
        InterfaceOption.MethodOption option = request.getOption();
        Invocation invocation = request.getPayLoad();
        RequestContext rpcContext = RequestContext.getContext();
        final ContextCarrier contextCarrier = new ContextCarrier();
        String operationName = option.getTraceSpanId(invocation);
        AbstractSpan span = ContextManager.createExitSpan(operationName, contextCarrier, null);
        CarrierItem next = contextCarrier.items();
        while (next.hasNext()) {
            next = next.next();
            rpcContext.getAttachments().put(next.getHeadKey(), next.getHeadValue());
            if (invocation.getAttachments().containsKey(next.getHeadKey())) {
                invocation.getAttachments().remove(next.getHeadKey());
            }
        }

        //Tags.URL.set(span, generateRequestURL(url, operationName));

        span.setComponent(new OfficialComponent(ID, getProtocol()));
        SpanLayer.asRPCFramework(span);
    }

    @Override
    public Object afterMethod(final EnhancedInstance objInst, final Method method, final Object[] allArguments,
                              final Class<?>[] argumentsTypes, final Object ret) throws Throwable {
        ContextSnapshot snapshot = ContextManager.capture();
        boolean isAsync = (Boolean) allArguments[2];
        if (isAsync) {
            CompletableFuture<Object> future = (CompletableFuture<Object>) ret;
            future.whenComplete((result, error) -> {
                if (error != null) {
                    dealException(error);
                } else {
                    ContextManager.continued(snapshot);
                    ContextManager.stopSpan();
                }
            });
        } else {
            ContextManager.stopSpan();
        }
        return ret;
    }

    @Override
    public void handleMethodException(final EnhancedInstance objInst, final Method method, final Object[] allArguments,
                                      final Class<?>[] argumentsTypes, final Throwable t) {
        dealException(t);
    }

    /**
     * 异常
     *
     * @param throwable
     */
    protected void dealException(final Throwable throwable) {
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(throwable);
    }

    /**
     * Format request url. e.g. dubbo://127.0.0.1:20880/org.apache.skywalking.apm.plugin.test.Test.test(String).
     *
     * @return request url.
     */
    protected String generateRequestURL(URL url, String operationName) {
        StringBuilder builder = new StringBuilder();
        builder.append(url.getProtocol() + "://");
        builder.append(url.getHost());
        builder.append(":" + url.getPort() + "/");
        builder.append(operationName);
        return builder.toString();
    }
}
