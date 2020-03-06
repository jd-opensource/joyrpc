package io.joyrpc.spring.context;

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

import io.joyrpc.context.EnvironmentSupplier;
import io.joyrpc.extension.Extension;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashMap;
import java.util.Map;

import static io.joyrpc.context.EnvironmentSupplier.SPRING_ORDER;

/**
 * Spring环境变量
 *
 * @description:
 */
@Extension(value = "spring", order = SPRING_ORDER)
public class SpringEnvironmentSupplier implements EnvironmentSupplier {

    public static final String BEAN_NAME = "springEnvironmentSupplier";
    public static final String SPRING_APPLICATION_NAME = "spring.application.name";
    public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

    /**
     * 环境配置
     */
    protected ConfigurableEnvironment environment;

    public SpringEnvironmentSupplier(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Map<String, String> environment() {
        Map<String, String> result = new HashMap<>();
        result.put(SPRING_APPLICATION_NAME, environment.getProperty(SPRING_APPLICATION_NAME));
        result.put(SPRING_PROFILES_ACTIVE, environment.getProperty(SPRING_PROFILES_ACTIVE));
        return result;
    }
}
