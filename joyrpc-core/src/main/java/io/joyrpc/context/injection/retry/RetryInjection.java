package io.joyrpc.context.injection.retry;

import io.joyrpc.cluster.Node;
import io.joyrpc.context.RequestContext.InnerContext;
import io.joyrpc.context.injection.NodeReqInjection;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import static io.joyrpc.constants.Constants.INTERNAL_KEY_RETRY_TIMES;
import static io.joyrpc.context.RequestContext.ALL;

/**
 * 重试次数注入
 */
@Extension("retry")
public class RetryInjection implements NodeReqInjection {
    @Override
    public boolean test() {
        return true;
    }

    @Override
    public void inject(RequestMessage<Invocation> request, Node node) {
        int retryTimes = request.getRetryTimes();
        if (retryTimes > 0) {
            InnerContext context = new InnerContext(request.getContext());
            context.setAttachment(INTERNAL_KEY_RETRY_TIMES, retryTimes, ALL);
        }
    }
}
