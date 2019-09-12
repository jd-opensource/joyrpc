package io.joyrpc.transport.netty4.http2;

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
import io.joyrpc.transport.codec.CodecContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @date: 2019/4/12
 */
public class Http2CodecContext implements CodecContext {

    protected Map<String, Object> attrs = new ConcurrentHashMap<>();

    protected Channel channel;

    protected Http2CodecContext(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public <T> T getAttr(String key) {
        return (T) attrs.get(key);
    }

    @Override
    public <T> T setAttr(String key, T value) {
        return (T) attrs.put(key, value);
    }
}
