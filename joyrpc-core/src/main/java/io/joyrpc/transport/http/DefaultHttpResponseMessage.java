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
 * 默认http应答消息
 */
public class DefaultHttpResponseMessage implements HttpResponseMessage {

    /**
     * 头部
     */
    protected HttpHeaders httpHeaders = new DefaultHttpHeaders();
    /**
     * 状态
     */
    protected int status;
    /**
     * 内容
     */
    protected byte[] content;

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public void setContent(byte[] bytes) {
        this.content = bytes;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public byte[] content() {
        return content;
    }

    @Override
    public HttpHeaders headers() {
        return httpHeaders;
    }
}
