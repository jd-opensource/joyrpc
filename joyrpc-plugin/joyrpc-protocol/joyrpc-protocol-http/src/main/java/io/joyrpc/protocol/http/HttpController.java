package io.joyrpc.protocol.http;

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

import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.URL;
import io.joyrpc.transport.channel.ChannelContext;
import io.joyrpc.transport.http.HttpRequestMessage;

import java.util.List;

/**
 * HTTP请求控制器，提供扩展点，可以增加Swagger等插件
 */
@Extensible("HttpController")
public interface HttpController {

    /**
     * 上下文
     *
     * @param ctx     上下文
     * @param message 消息
     * @param url     url
     * @param params  参数名称
     * @return 返回对象
     * @throws Exception 异常
     */
    Object execute(ChannelContext ctx, HttpRequestMessage message, URL url, List<String> params) throws Exception;

    /**
     * 保持相对路径
     *
     * @return 保持相对路径标识
     */
    default boolean relativePath() {
        return true;
    }
}
