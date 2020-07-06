package io.joyrpc.context.injection.retry;

import io.joyrpc.cluster.Node;
import io.joyrpc.context.injection.NodeReqInjection;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import static io.joyrpc.constants.Constants.INTERNAL_KEY_RETRY_TIMES;

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
    public void inject(final RequestMessage<Invocation> request, final Node node) {
        int retryTimes = request.getRetryTimes();
        if (retryTimes > 0) {
            Invocation invocation = request.getPayLoad();
            invocation.addAttachment(INTERNAL_KEY_RETRY_TIMES, retryTimes);
        }
    }
}
