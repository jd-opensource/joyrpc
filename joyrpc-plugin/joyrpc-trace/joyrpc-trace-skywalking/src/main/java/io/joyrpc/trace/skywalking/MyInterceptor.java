package io.joyrpc.trace.skywalking;

import io.joyrpc.Invoker;
import io.joyrpc.context.RequestContext;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import org.apache.skywalking.apm.agent.core.context.CarrierItem;
import org.apache.skywalking.apm.agent.core.context.ContextCarrier;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.tag.Tags;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;

/**
 * 拦截器
 */
public class MyInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        Invoker invoker = (Invoker) allArguments[0];
        RequestMessage<Invocation> request = (RequestMessage<Invocation>) allArguments[1];
        Invocation invocation = (Invocation) allArguments[1];
        RequestContext rpcContext = RequestContext.getContext();
        boolean isConsumer = true;//rpcContext.isConsumerSide();
        URL requestURL = null;//invoker.getUrl();

        AbstractSpan span;

        final String host = requestURL.getHost();
        final int port = requestURL.getPort();
        if (isConsumer) {
            final ContextCarrier contextCarrier = new ContextCarrier();
            span = ContextManager.createExitSpan(generateOperationName(requestURL, invocation), contextCarrier, host + ":" + port);
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                rpcContext.getAttachments().put(next.getHeadKey(), next.getHeadValue());
                if (invocation.getAttachments().containsKey(next.getHeadKey())) {
                    invocation.getAttachments().remove(next.getHeadKey());
                }
            }
        } else {
            ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                next.setHeadValue(rpcContext.getAttachment(next.getHeadKey()));
            }

            span = ContextManager.createEntrySpan(generateOperationName(requestURL, invocation), contextCarrier);
        }

        Tags.URL.set(span, generateRequestURL(requestURL, invocation));
        span.setComponent(ComponentsDefine.DUBBO);
        SpanLayer.asRPCFramework(span);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
//        Result result = (Result) ret;
//        if (result != null && result.getException() != null) {
//            dealException(result.getException());
//        }

        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        dealException(t);
    }

    /**
     * Log the throwable, which occurs in Dubbo RPC service.
     */
    private void dealException(Throwable throwable) {
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(throwable);
    }

    /**
     * Format operation name. e.g. org.apache.skywalking.apm.plugin.test.Test.test(String)
     *
     * @return operation name.
     */
    private String generateOperationName(URL requestURL, Invocation invocation) {
        StringBuilder operationName = new StringBuilder();
        operationName.append(requestURL.getPath());
        operationName.append("." + invocation.getMethodName() + "(");
        for (Class<?> classes : invocation.getArgClasses()) {
            operationName.append(classes.getSimpleName() + ",");
        }

        if (invocation.getArgClasses().length > 0) {
            operationName.delete(operationName.length() - 1, operationName.length());
        }

        operationName.append(")");

        return operationName.toString();
    }

    /**
     * Format request url. e.g. dubbo://127.0.0.1:20880/org.apache.skywalking.apm.plugin.test.Test.test(String).
     *
     * @return request url.
     */
    private String generateRequestURL(URL url, Invocation invocation) {
        StringBuilder requestURL = new StringBuilder();
        requestURL.append(url.getProtocol() + "://");
        requestURL.append(url.getHost());
        requestURL.append(":" + url.getPort() + "/");
        requestURL.append(generateOperationName(url, invocation));
        return requestURL.toString();
    }
}
