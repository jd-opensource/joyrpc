package io.joyrpc.invoker.exception;

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

import io.joyrpc.cluster.event.OfflineEvent;
import io.joyrpc.cluster.event.SessionLostEvent;
import io.joyrpc.event.Publisher;
import io.joyrpc.exception.SessionException;
import io.joyrpc.exception.ShutdownExecption;
import io.joyrpc.extension.Extension;
import io.joyrpc.transport.Client;
import io.joyrpc.transport.event.TransportEvent;

import static io.joyrpc.invoker.exception.ExceptionHandler.SYSTEM_ORDER;

/**
 * 系统异常处理器
 */
@Extension(value = "system", order = SYSTEM_ORDER)
public class SystemExceptionHandler implements ExceptionHandler {

    @Override
    public void handle(final Client client, final Throwable throwable) {
        Publisher<TransportEvent> publisher = client.getPublisher();
        if (publisher != null) {
            if (throwable instanceof SessionException) {
                publisher.offer(new SessionLostEvent(client));
            } else if (throwable instanceof ShutdownExecption) {
                publisher.offer(new OfflineEvent(client));
            }
        }
    }
}
