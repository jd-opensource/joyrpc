package io.joyrpc.transport;

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

import io.joyrpc.event.AsyncResult;
import io.joyrpc.exception.ConnectionException;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.Channel;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 共享服务，采用引用计数器。<p/>
 * 只要调用Open就增加计数器，不管是否抛出异常。<br>
 * 调用Close就减少计数器，当计数器为0才真正关闭
 */
public class ShareServer extends DecoratorServer<Server> {

    protected AtomicLong counter = new AtomicLong(0);
    /**
     * 真正关闭之前的清理消费者
     */
    protected Consumer<Server> closing;

    public ShareServer(Server server, Consumer<Server> closing) {
        super(server);
        this.closing = closing;
    }

    public ShareServer(URL url, Server transport, Consumer<Server> closing) {
        super(url, transport);
        this.closing = closing;
    }

    @Override
    public Channel open() throws ConnectionException, InterruptedException {
        counter.incrementAndGet();
        return super.open();
    }

    @Override
    public void open(final Consumer<AsyncResult<Channel>> consumer) {
        counter.incrementAndGet();
        super.open(consumer);
    }

    @Override
    public void close() throws Exception {
        if (counter.decrementAndGet() == 0) {
            super.close();
        }
    }

    @Override
    public void close(final Consumer<AsyncResult<Channel>> consumer) {
        if (counter.decrementAndGet() == 0) {
            if (closing != null) {
                closing.accept(this);
            }
            super.close(consumer);
        } else if (consumer != null) {
            consumer.accept(new AsyncResult<>(getServerChannel()));
        }
    }
}
