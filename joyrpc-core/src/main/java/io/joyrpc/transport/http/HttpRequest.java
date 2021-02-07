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
 * http请求消息
 */
public class HttpRequest extends DefaultHttpRequestMessage {

    public static final String DEFLATE = "deflate";
    public static final String CHARSET_NAME = "UTF-8"; //服务端交互的编码

    /**
     * 连接超时
     */
    protected int connectTimeout = 5000;

    /**
     * 数据包超时
     */
    protected int socketTimeout = 5000;

    public HttpRequest() {
        super();
    }

    public HttpRequest(String uri, HttpMethod httpMethod) {
        super(uri, httpMethod, null, null);
    }

    public HttpRequest(String uri, HttpMethod httpMethod, HttpHeaders httpHeaders, byte[] content) {
        super(uri, httpMethod, httpHeaders, content);
    }

    public HttpRequest(String uri, HttpMethod httpMethod, HttpHeaders httpHeaders) {
        super(uri, httpMethod, httpHeaders, null);
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setHttpHeaders(HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }
}
