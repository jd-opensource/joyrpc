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
import io.joyrpc.config.ConsumerGroupConfig;
import io.joyrpc.config.ProviderConfig;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

/**
 * 插件验证
 */
public class AliasValidator implements ConstraintValidator<ValidateAlias, AbstractInterfaceConfig> {
    /**
     * 可用的字符串为：英文大小写，数字，横杆-，下划线_，点. 冒号:
     * !@#$*,;有特殊含义
     */
    public final static Pattern NORMAL_COLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.:]+$");
    public final static Pattern NORMAL_COMMA_COLON = Pattern.compile("^[a-zA-Z0-9\\-\\_\\.,:]+$");

    @Override
    public boolean isValid(final AbstractInterfaceConfig value, final ConstraintValidatorContext context) {
        String alias = value.getAlias();
        String message = null;
        if (alias == null || alias.isEmpty()) {
            message = "alias can not be empty.";
        } else if (value instanceof ConsumerGroupConfig) {
            if (!NORMAL_COMMA_COLON.matcher(alias).matches()) {
                message = "alias \'" + alias + "\' is invalid. only allow a-zA-Z0-9 '-' '_' '.' ':' ','";
            }
        } else if (value instanceof AbstractConsumerConfig) {
            if (!NORMAL_COLON.matcher(alias).matches()) {
                message = "alias \'" + alias + "\' is invalid. only allow a-zA-Z0-9 '-' '_' '.' ':'";
            }
        } else if (value instanceof ProviderConfig) {
            if (!NORMAL_COLON.matcher(alias).matches()) {
                message = "alias \'" + alias + "\' is invalid. only allow a-zA-Z0-9 '-' '_' '.' ':'";
            }
        }
        if (message != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(message).addPropertyNode("alias").addConstraintViolation();
            return false;
        }
        return true;
    }

    @Override
    public void initialize(final ValidateAlias group) {
    }
}
