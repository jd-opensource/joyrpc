package io.joyrpc.invoker.injection;

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

import io.joyrpc.Result;
import io.joyrpc.annotation.Ignore;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import static io.joyrpc.util.ClassUtils.withoutAnnotation;

/**
 * 透传插件包装器
 */
public class Transmits implements Transmit {
    /**
     * 透传插件
     */
    protected Iterable<Transmit>[] transmits = new Iterable[6];

    public Transmits(final Iterable<Transmit> transmits) {
        this.transmits[0] = transmits;
        this.transmits[1] = withoutAnnotation(transmits, METHOD_REJECT, Ignore.class);
        this.transmits[2] = withoutAnnotation(transmits, METHOD_ON_RETURN, Ignore.class);
        this.transmits[3] = withoutAnnotation(transmits, METHOD_ON_COMPLETE, Ignore.class);
        this.transmits[4] = withoutAnnotation(transmits, METHOD_ON_SERVER_RETURN, Ignore.class);
        this.transmits[5] = withoutAnnotation(transmits, METHOD_ON_SERVER_COMPLETE, Ignore.class);
    }

    @Override
    public void inject(final RequestMessage<Invocation> request) {
        for (Transmit transmit : transmits[0]) {
            transmit.inject(request);
        }
    }

    @Override
    public void reject(final RequestMessage<Invocation> request) {
        if (transmits[1] != null) {
            for (Transmit transmit : transmits[1]) {
                transmit.reject(request);
            }
        }
    }

    @Override
    public void onReturn(final RequestMessage<Invocation> request) {
        if (transmits[2] != null) {
            for (Transmit transmit : transmits[2]) {
                transmit.onReturn(request);
            }
        }
    }

    @Override
    public void onComplete(final RequestMessage<Invocation> request, final Result result) {
        if (transmits[3] != null) {
            for (Transmit transmit : transmits[3]) {
                transmit.onComplete(request, result);
            }
        }
    }

    @Override
    public void onServerReceive(final RequestMessage<Invocation> request) {
        for (Transmit transmit : transmits[0]) {
            transmit.onServerReceive(request);
        }
    }

    @Override
    public void onServerReturn(final RequestMessage<Invocation> request) {
        if (transmits[4] != null) {
            for (Transmit transmit : transmits[4]) {
                transmit.onServerReturn(request);
            }
        }
    }

    @Override
    public void onServerComplete(final RequestMessage<Invocation> request, final Result result) {
        if (transmits[5] != null) {
            for (Transmit transmit : transmits[5]) {
                transmit.onServerComplete(request, result);
            }
        }
    }
}
