package io.joyrpc.protocol.message.authentication;

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

import io.joyrpc.protocol.message.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证请求
 */
public class AuthenticationRequest implements Request {

    private static final long serialVersionUID = 7814724391331498463L;
    /**
     * 类型
     */
    protected String type;
    /**
     * 扩展属性
     */
    protected Map<String, String> attributes = new HashMap<>();

    public AuthenticationRequest() {
    }

    public AuthenticationRequest(String type) {
        this.type = type;
    }

    public AuthenticationRequest(String type, Map<String, String> attributes) {
        this.type = type;
        this.attributes = attributes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * 添加扩展属性
     *
     * @param key
     * @param value
     */
    public void addAttribute(final String key, final String value) {
        if (key != null && value != null) {
            attributes.put(key, value);
        }
    }

    /**
     * 删除扩展属性
     *
     * @param key
     * @return
     */
    public String removeAttribute(String key) {
        return key == null ? null : attributes.remove(key);
    }
}
