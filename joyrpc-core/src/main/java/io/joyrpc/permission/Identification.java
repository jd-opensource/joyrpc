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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.Parametric;
import io.joyrpc.extension.Type;

import java.util.Map;

/**
 * 身份提供者
 */
@Extension("identification")
public interface Identification extends Type<String> {

    /**
     * 提供身份信息
     *
     * @param parametric 参数信息
     * @return 认证请求
     */
    Map<String, String> identity(Parametric parametric);

}
