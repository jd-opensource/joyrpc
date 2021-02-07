package io.joyrpc.transport.http2;

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

import io.joyrpc.transport.http.HttpHeaders;
import io.joyrpc.transport.http.HttpMethod;

import static io.joyrpc.transport.http2.Http2Headers.PseudoHeaderName.*;

/**
 * 默认http2头
 */
public interface Http2Headers extends HttpHeaders {

    enum PseudoHeaderName {
        /**
         * 方法
         * {@code :method}.
         */
        METHOD(":method"),

        /**
         * Scheme
         * {@code :scheme}.
         */
        SCHEME(":scheme"),

        /**
         * 权限
         * {@code :authority}.
         */
        AUTHORITY(":authority"),

        /**
         * 路径
         * {@code :path}.
         */
        PATH(":path"),

        /**
         * 状态
         * {@code :status}.
         */
        STATUS(":status");

        private String value;

        PseudoHeaderName(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

    }

    /**
     * 获取 method
     *
     * @return HttpMethod
     */
    default HttpMethod method() {
        Object methodName = get(METHOD.value);
        return methodName == null ? HttpMethod.POST : HttpMethod.valueOf(methodName.toString());
    }

    /**
     * 设置 method
     *
     * @param method HttpMethod
     * @return Http2Headers
     */
    default Http2Headers method(HttpMethod method) {
        set(METHOD.value, method.name());
        return this;
    }

    /**
     * 设置 method
     *
     * @param method method
     * @return Http2Headers
     */
    default Http2Headers method(CharSequence method) {
        set(METHOD.value, method);
        return this;
    }

    /**
     * 获取 path
     *
     * @return String
     */
    default CharSequence path() {
        Object path = get(PATH.value);
        return path == null ? "/" : path.toString();
    }

    /**
     * 设置path
     *
     * @param path path
     * @return Http2Headers
     */
    default Http2Headers path(CharSequence path) {
        set(PATH.value, path);
        return this;
    }

    /**
     * 获取 status
     *
     * @return CharSequence
     */
    default CharSequence status() {
        Object status = get(STATUS.value);
        return status == null ? "200" : status.toString();
    }

    /**
     * 设置status
     *
     * @param status CharSequence
     * @return Http2Headers
     */
    default Http2Headers status(CharSequence status) {
        set(STATUS.value, status);
        return this;
    }

    /**
     * 获取 scheme
     *
     * @return CharSequence
     */
    default CharSequence scheme() {
        Object scheme = get(SCHEME.value);
        return scheme == null ? null : scheme.toString();
    }

    /**
     * 设置 scheme
     *
     * @param scheme CharSequence
     * @return Http2Headers
     */
    default Http2Headers scheme(CharSequence scheme) {
        set(SCHEME.value, scheme);
        return this;
    }


    /**
     * 获取 authority
     *
     * @return CharSequence
     */
    default CharSequence authority() {
        Object authority = get(AUTHORITY.value);
        return authority == null ? null : authority.toString();
    }

    /**
     * 设置 authority
     *
     * @param authority CharSequence
     * @return Http2Headers
     */
    default Http2Headers authority(CharSequence authority) {
        set(AUTHORITY.value, authority);
        return this;
    }


}
