package io.joyrpc.transport.http;

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

/**
 * http应答消息
 */
public interface HttpResponseMessage extends HttpMessage {

    /**
     * 设置状态
     *
     * @param status 状态
     */
    void setStatus(int status);

    /**
     * 获取状态
     *
     * @return 状态
     */
    int getStatus();

    /**
     * 设置内容
     *
     * @param bytes 内容
     */
    void setContent(byte[] bytes);

}
