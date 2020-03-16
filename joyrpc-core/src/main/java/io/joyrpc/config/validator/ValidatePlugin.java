package io.joyrpc.config.validator;

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

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Constraint(validatedBy = PluginValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ValidatePlugin {

    String message() default "";

    /**
     * 扩展类
     *
     * @return 扩展类
     */
    Class<?> extensible();

    /**
     * 定义插件的类，默认是"io.joyrpc.Plugin"
     *
     * @return 定义插件的类
     */
    Class<?> definition() default io.joyrpc.Plugin.class;

    /**
     * 在插件定义类中扩展点名称
     *
     * @return 扩展点名称
     */
    String name();

    /**
     * 默认值
     *
     * @return 默认值
     */
    String defaultValue() default "";

    /**
     * 当配置的插件不存在时候，选择最大权重的可用插件
     *
     * @return 后续标识</ br>
     * <li>true 当配置的插件不存在时候，选择最大权重的可用插件</li>
     * <li>false 当配置的插件不存在时候，不选择最大权重的可用插件</li>
     */
    boolean candidate() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
