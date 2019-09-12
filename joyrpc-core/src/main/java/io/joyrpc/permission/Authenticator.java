package io.joyrpc.permission;

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
import io.joyrpc.extension.Type;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.authentication.AuthenticationRequest;
import io.joyrpc.protocol.message.authentication.AuthenticationResponse;

/**
 * 认证接口
 */
@Extensible("authenticator")
public interface Authenticator extends Type<String> {

    /**
     * 判断参数是否足够
     *
     * @param url
     * @return
     */
    boolean support(URL url);

    /**
     * 构造身份请求
     *
     * @param url
     * @return
     */
    AuthenticationRequest identity(URL url);

    /**
     * 进行认证
     *
     * @param request
     */
    AuthenticationResponse authenticate(URL url, AuthenticationRequest request);

}
