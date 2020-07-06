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
import io.joyrpc.extension.Parametric;
import io.joyrpc.permission.Identification;

import java.util.HashMap;
import java.util.Map;

/**
 * 令牌身份
 */
public class TokenIdentification implements Identification {

    @Override
    public Map<String, String> identity(final Parametric parametric) {
        String token = parametric.getString(Constants.HIDDEN_KEY_TOKEN);
        if (token == null) {
            return null;
        }
        Map<String, String> result = new HashMap<>(1);
        result.put(Constants.HIDDEN_KEY_TOKEN, token);
        return result;
    }

    @Override
    public String type() {
        return "token";
    }
}
