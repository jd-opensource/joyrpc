package io.joyrpc.filter.consumer;

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
import io.joyrpc.filter.AbstractValidationFilter;
import io.joyrpc.filter.ConsumerFilter;

/**
 * Title: 参数校验过滤器<br>
 *
 * @description: 支持接口级或者方法级配置，服务端和客户端都可以配置，需要引入第三方jar包<br>
 */
@Extension(value = "validation", order = ConsumerFilter.VALIDATION_ORDER)
public class ValidationFilter extends AbstractValidationFilter implements ConsumerFilter {

    @Override
    public int type() {
        return SYSTEM_GLOBAL;
    }
}
