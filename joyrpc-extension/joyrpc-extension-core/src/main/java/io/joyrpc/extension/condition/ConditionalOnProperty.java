package io.joyrpc.extension.condition;

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

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnPropertyCondition.class)
public @interface ConditionalOnProperty {
    /**
     * 值
     *
     * @return 值
     */
    String value() default "";

    /**
     * 名称
     *
     * @return 名称
     */
    String name() default "";

    /**
     * 不存在是否匹配
     *
     * @return 不存在是否匹配标识
     */
    boolean matchIfMissing() default false;

}
