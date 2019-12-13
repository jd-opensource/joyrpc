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

import io.joyrpc.config.AbstractConsumerConfig;
import io.joyrpc.config.AbstractInterfaceConfig;
import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.filter.ConsumerFilter;
import io.joyrpc.filter.Filter;
import io.joyrpc.filter.ProviderFilter;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static io.joyrpc.Plugin.CONSUMER_FILTER;
import static io.joyrpc.Plugin.PROVIDER_FILTER;
import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 过滤器验证
 */
public class FilterValidator implements ConstraintValidator<ValidateFilter, AbstractInterfaceConfig> {

    @Override
    public boolean isValid(final AbstractInterfaceConfig config, final ConstraintValidatorContext context) {

        String value = config.getFilter();
        if (value != null && !value.isEmpty()) {
            String message = null;
            ExtensionPoint<? extends Filter, String> point = config instanceof AbstractConsumerConfig ? CONSUMER_FILTER : PROVIDER_FILTER;
            Class clazz = config instanceof AbstractConsumerConfig ? ConsumerFilter.class : ProviderFilter.class;
            String[] values = split(value, SEMICOLON_COMMA_WHITESPACE);
            for (String v : values) {
                if (v.charAt(0) != '-' && null == point.get(v)) {
                    //过滤掉黑名单
                    message = String.format("No such extension \'%s\' of %s. ", v, clazz.getName());
                    break;
                }
            }
            if (message != null) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode("filter")
                        .addConstraintViolation();
                return false;
            }
        }
        return true;
    }

    @Override
    public void initialize(final ValidateFilter validate) {
    }
}
