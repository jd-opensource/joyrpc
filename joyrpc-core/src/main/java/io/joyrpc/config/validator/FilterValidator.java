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

import io.joyrpc.config.AbstractInterfaceConfig;
import io.joyrpc.extension.MapParametric;
import io.joyrpc.extension.Parametric;
import io.joyrpc.invoker.chain.FilterChainFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static io.joyrpc.Plugin.FILTER_CHAIN_FACTORY;
import static io.joyrpc.constants.Constants.FILTER_CHAIN_FACTORY_OPTION;

/**
 * 过滤器验证
 */
public class FilterValidator implements ConstraintValidator<ValidateFilter, AbstractInterfaceConfig> {

    @Override
    public boolean isValid(final AbstractInterfaceConfig config, final ConstraintValidatorContext context) {
        //判断插件配置
        String value = config.getFilter();
        if (value != null && !value.isEmpty()) {
            Parametric parametric = new MapParametric(config.getParameters());
            FilterChainFactory chainFactory = FILTER_CHAIN_FACTORY.getOrDefault(parametric.getString(FILTER_CHAIN_FACTORY_OPTION));
            String message = chainFactory.validate(config);
            if (message != null && !message.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode("filter")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
