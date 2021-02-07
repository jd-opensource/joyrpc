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
 * 默认http请求消息
 */
public class DefaultHttpRequestMessage implements HttpRequestMessage {
    /**
     * uri
     */
    protected String uri;
    /**
     * 方法
     */
    protected HttpMethod httpMethod;
    /**
     * 头部
     */
    protected HttpHeaders httpHeaders;
    /**
     * 内容
     */
    protected byte[] content;

    public DefaultHttpRequestMessage() {
        content = new byte[0];
    }

    public DefaultHttpRequestMessage(String uri, HttpMethod httpMethod, HttpHeaders httpHeaders, byte[] content) {
        this.uri = uri;
        this.httpMethod = httpMethod;
        this.httpHeaders = httpHeaders;
        this.content = content == null ? new byte[0] : content;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public HttpMethod getHttpMethod() {
        return httpMethod;
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
