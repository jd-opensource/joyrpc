package io.joyrpc.permission.token;

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

import io.joyrpc.InvokerAware;
import io.joyrpc.constants.Constants;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.permission.Authentication;
import io.joyrpc.protocol.message.authentication.AuthenticationRequest;
import io.joyrpc.protocol.message.authentication.AuthenticationResponse;

import java.util.Map;

import static io.joyrpc.protocol.message.authentication.AuthenticationResponse.NOT_PASS;
import static io.joyrpc.protocol.message.authentication.AuthenticationResponse.PASS;

/**
 * 令牌认证
 */
@Extension("token")
public class TokenAuthentication implements Authentication, InvokerAware {

    /**
     * URL
     */
    protected URL url;
    /**
     * 接口类，在泛型调用情况下，clazz和clazzName可能不相同
     */
    protected Class clazz;
    /**
     * 接口类名
     */
    protected String className;
    /**
     * 认证
     */
    protected String token;

    @Override
    public void setUrl(URL url) {
        this.url = url;
    }

    @Override
    public void setClass(Class clazz) {
        this.clazz = clazz;
    }

    @Override
    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public void setup() {
        token = url.getString(Constants.HIDDEN_KEY_TOKEN);
    }

    @Override
    public AuthenticationResponse authenticate(final AuthenticationRequest request) {
        Map<String, String> attributes = request.getAttributes();
        boolean pass = token == null || token.isEmpty() || token.equals(attributes == null ? null : attributes.get(Constants.HIDDEN_KEY_TOKEN));
        return new AuthenticationResponse(pass ? PASS : NOT_PASS, pass ? null : "token is invalid.");
    }

}
