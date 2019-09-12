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

import io.joyrpc.constants.Constants;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.permission.Authenticator;
import io.joyrpc.protocol.message.authentication.AuthenticationRequest;
import io.joyrpc.protocol.message.authentication.AuthenticationResponse;

import static io.joyrpc.protocol.message.authentication.AuthenticationResponse.NOT_PASS;
import static io.joyrpc.protocol.message.authentication.AuthenticationResponse.PASS;

@Extension
public class TokenAuthenticator implements Authenticator {
    @Override
    public boolean support(final URL url) {
        String token = url.getString(Constants.HIDDEN_KEY_TOKEN);
        return token != null && !token.isEmpty();
    }

    @Override
    public AuthenticationRequest identity(final URL url) {
        AuthenticationRequest result = new AuthenticationRequest(type());
        result.addAttribute(Constants.HIDDEN_KEY_TOKEN, url.getString(Constants.HIDDEN_KEY_TOKEN));
        return result;
    }

    @Override
    public AuthenticationResponse authenticate(final URL url, final AuthenticationRequest request) {
        String token = url.getString(Constants.HIDDEN_KEY_TOKEN);
        boolean pass = token == null || token.isEmpty() || token.equals(request.getAttributes().get(Constants.HIDDEN_KEY_TOKEN));
        return new AuthenticationResponse(pass ? PASS : NOT_PASS, pass ? null : "token is invalid.");
    }

    @Override
    public String type() {
        return "token";
    }
}
