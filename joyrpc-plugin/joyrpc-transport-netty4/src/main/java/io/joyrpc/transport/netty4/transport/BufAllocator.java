package io.joyrpc.transport.netty4.transport;

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

import io.joyrpc.extension.URL;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;

import static io.joyrpc.constants.Constants.BUFFER_POOLED_OPTION;
import static io.joyrpc.constants.Constants.BUFFER_PREFER_DIRECT_KEY;

/**
 * 缓冲区分配器
 */
public class BufAllocator {

    /**
     * 创建缓冲区分配器
     *
     * @param url
     * @return
     */
    public static ByteBufAllocator create(final URL url) {
        return url.getBoolean(BUFFER_POOLED_OPTION) ? createPooled(url) : createUnPooled(url);
    }

    /**
     * 创建缓存的缓冲区
     *
     * @param url
     * @return
     */
    protected static ByteBufAllocator createPooled(final URL url) {
        String preferDirect = url.getString(BUFFER_PREFER_DIRECT_KEY);
        if ("true".equalsIgnoreCase(preferDirect)) {
            return new PooledByteBufAllocator(true);
        } else if ("false".equalsIgnoreCase(preferDirect)) {
            return new PooledByteBufAllocator(false);
        } else {
            return PooledByteBufAllocator.DEFAULT;
        }
    }

    /**
     * 创建非缓存的缓冲区
     *
     * @param url
     * @return
     */
    protected static ByteBufAllocator createUnPooled(final URL url) {
        String preferDirect = url.getString(BUFFER_PREFER_DIRECT_KEY);
        if ("true".equalsIgnoreCase(preferDirect)) {
            return new UnpooledByteBufAllocator(true);
        } else if ("false".equalsIgnoreCase(preferDirect)) {
            return new UnpooledByteBufAllocator(false);
        } else {
            return UnpooledByteBufAllocator.DEFAULT;
        }
    }
}
