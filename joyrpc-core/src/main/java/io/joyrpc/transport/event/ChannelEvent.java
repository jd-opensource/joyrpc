package io.joyrpc.transport.event;

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

import io.joyrpc.transport.channel.Channel;

/**
 * @date: 2019/3/4
 */
public class ChannelEvent implements TransportEvent {

    protected Channel channel;

    protected boolean success;

    protected Throwable throwable;

    public ChannelEvent(Channel channel) {
        this.channel = channel;
        this.success = true;
    }

    public ChannelEvent(Throwable throwable) {
        this.success = false;
        this.throwable = throwable;
    }

    public ChannelEvent(Channel channel, Throwable throwable) {
        this.success = false;
        this.channel = channel;
        this.throwable = throwable;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isSuccess() {
        return success;
    }

    public Throwable getThrowable() {
        return throwable;
    }

}
