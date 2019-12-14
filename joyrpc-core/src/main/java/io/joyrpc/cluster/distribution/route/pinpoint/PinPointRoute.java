package io.joyrpc.cluster.distribution.route.pinpoint;

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

import io.joyrpc.cluster.Candidate;
import io.joyrpc.cluster.Node;
import io.joyrpc.cluster.distribution.Route;
import io.joyrpc.cluster.distribution.route.AbstractRoute;
import io.joyrpc.context.RequestContext;
import io.joyrpc.exception.IllegalConfigureException;
import io.joyrpc.exception.NoAliveProviderException;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.util.StringUtils;

import java.util.concurrent.CompletableFuture;

import static io.joyrpc.cluster.distribution.Route.PIN_POINT;
import static io.joyrpc.constants.Constants.HIDDEN_KEY_PINPOINT;
import static io.joyrpc.constants.ExceptionCode.COMMON_VALUE_ILLEGAL;
import static io.joyrpc.constants.ExceptionCode.CONSUMER_NO_ALIVE_PROVIDER;

/**
 * 定点调用
 */
@Extension(value = PIN_POINT, order = Route.ORDER_PINOINT)
public class PinPointRoute<T, R> extends AbstractRoute<T, R> {

    @Override
    public CompletableFuture<R> invoke(final T request, final Candidate candidate) {
        // todo 没法从request取
        String pinpoint = RequestContext.getContext().getAttachment(HIDDEN_KEY_PINPOINT);
        if (StringUtils.isBlank(pinpoint)) {
            throw new IllegalConfigureException("pinpoint must set pinpoint", COMMON_VALUE_ILLEGAL);
        }
        URL url = URL.valueOf(pinpoint);
        Node pinpointNode = null;
        for (Node node : candidate.getNodes()) {
            if (node.getUrl().toString(false, false).equals(url.toString(false, false))) {
                pinpointNode = node;
                break;
            }
        }
        if (null == pinpointNode) {
            throw new NoAliveProviderException(String.format("not found node %s in candidate", url.toString(false, false)), CONSUMER_NO_ALIVE_PROVIDER);
        }
        return function.apply(pinpointNode, null, request);
    }

}
