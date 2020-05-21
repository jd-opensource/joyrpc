package io.joyrpc.trace.skywalking;

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.context.GlobalContext;
import io.joyrpc.extension.URL;
import io.joyrpc.filter.AbstractFilter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.ContextSnapshot;
import org.apache.skywalking.apm.agent.core.context.tag.StringTag;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.*;

/**
 * 抽象的Skywalking插件
 */
public abstract class AbstractSkywalkingFilter extends AbstractFilter {

    protected static StringTag CONSUMER_ALIAS_TAG = new StringTag("consumer.alias");
    protected static StringTag CONSUMER_NAME_TAG = new StringTag("consumer.name");
    protected static StringTag CONSUMER_ADDRESS_TAG = new StringTag("consumer.address");

    protected String component;

    @Override
    public void setup() {
        component = GlobalContext.getString(PROTOCOL_KEY);
    }

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        beginTrace(request);
        ContextSnapshot snapshot = ContextManager.capture();
        CompletableFuture<Result> future = invoker.invoke(request);
        future.whenComplete((result, throwable) -> endTrace(snapshot, result, throwable));
        return future;
    }

    /**
     * 开启跟踪
     *
     * @param request 请求
     */
    protected void beginTrace(RequestMessage<Invocation> request) {

    }

    /**
     * 结束跟踪
     *
     * @param snapshot  快照
     * @param result    结果
     * @param throwable 异常
     */
    protected void endTrace(final ContextSnapshot snapshot, final Result result, final Throwable throwable) {
        ContextManager.continued(snapshot);
        Throwable error = throwable == null ? result.getException() : throwable;
        if (error != null) {
            AbstractSpan activeSpan = ContextManager.activeSpan();
            //异常
            activeSpan.errorOccurred();
            activeSpan.log(throwable);
        }
        ContextManager.stopSpan();
    }


    @Override
    public boolean test(final URL url) {
        if (url.getBoolean(TRACE_OPEN_OPTION)) {
            return true;
        }
        Map<String, String> tokens = url.endsWith("." + TRACE_OPEN);
        if (tokens.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            if (Boolean.parseBoolean(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}
