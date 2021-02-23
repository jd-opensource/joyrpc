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


import io.joyrpc.extension.URL;
import io.joyrpc.util.IdGenerator;

import java.net.InetSocketAddress;
import java.util.function.Supplier;

/**
 * 传输通道
 */
public interface Transport {

    /**
     * 获取URL
     *
     * @return url
     */
    URL getUrl();

    /**
     * 获取本地地址
     *
     * @return 本地地址
     */
    InetSocketAddress getLocalAddress();

    /**
     * 获取通道序号
     *
     * @return 通道序号
     */
    default int getTransportId() {
        return 0;
    }

    /**
     * 传输通道序号生成器
     */
    Supplier<Integer> ID_GENERATOR = new IdGenerator.IntIdGenerator();

}
