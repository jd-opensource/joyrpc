package io.joyrpc.protocol.http.injection;

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
import io.joyrpc.extension.Parametric;
import io.joyrpc.protocol.http.HeaderInjection;
import io.joyrpc.protocol.message.Invocation;


/**
 * 默认参数注入
 */
@Extension("default")
public class DefaultHeaderInjection implements HeaderInjection {

    public static final String X_HIDDEN_PREFIX = "X-HIDDEN-";
    public static final String X_TRANS_PREFIX = "X-TRANS-";

    @Override
    public void inject(final Invocation invocation, final Parametric header) {
        header.foreach((key, value) -> {
            if (!key.isEmpty()) {
                if (key.charAt(0) == Constants.HIDE_KEY_PREFIX) {
                    //hidden
                    invocation.addAttachment(key, value);
                } else if (key.startsWith(X_HIDDEN_PREFIX)) {
                    //hidden
                    invocation.addAttachment("." + key.substring(X_HIDDEN_PREFIX.length()), value);
                } else if (key.startsWith(X_TRANS_PREFIX)) {
                    //normal
                    invocation.addAttachment(key.substring(X_TRANS_PREFIX.length()), value);
                }
            }
        });
    }
}
