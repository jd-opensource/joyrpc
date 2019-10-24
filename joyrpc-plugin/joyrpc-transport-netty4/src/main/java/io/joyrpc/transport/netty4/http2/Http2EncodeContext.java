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
import io.joyrpc.transport.codec.EncodeContext;

/**
 * @date: 2019/4/12
 */
public class Http2EncodeContext extends Http2CodecContext implements EncodeContext {

    public Http2EncodeContext(Channel channel) {
        super(channel);
    }

    /**
     * 设置属性
     *
     * @param key
     * @param value
     * @param <T>
     * @return
     */
    public <T> Http2EncodeContext attribute(String key, T value) {
        setAttr(key, value);
        return this;
    }
}
