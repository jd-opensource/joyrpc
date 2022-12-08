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
import io.joyrpc.config.ConsumerGroupConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.util.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

import static io.joyrpc.constants.Constants.ALIAS_EMPTY_OPTION;
import static io.joyrpc.context.Variable.VARIABLE;

/**
 * 插件验证
 */
public class AliasValidator implements ConstraintValidator<ValidateAlias, AbstractInterfaceConfig> {

    @Override
    public boolean isValid(final AbstractInterfaceConfig value, final ConstraintValidatorContext context) {
        String regex = VARIABLE.getString(Constants.ALIAS_PATTERN_OPTION);
        Pattern pattern = Pattern.compile(regex);
        String alias = value.getAlias();
        String message = null;
        if (alias == null || alias.isEmpty()) {
            if (VARIABLE.getBoolean(ALIAS_EMPTY_OPTION)) {
                if (alias == null) {
                    //设置为空字符串，防止空指针异常
                    value.setAlias("");
                }
                return true;
            }
            message = "alias can not be empty.";
        } else if (value instanceof ConsumerGroupConfig) {
            //多个分组
            String[] parts = StringUtils.split(alias, StringUtils.SEMICOLON_COMMA_WHITESPACE);
            for (String part : parts) {
                if (!pattern.matcher(part).matches()) {
                    message = "alias \'" + alias + "\' is not match " + regex;
                    break;
                }
            }
        } else if (!pattern.matcher(alias).matches()) {
            message = "alias \'" + alias + "\' is not match " + regex;
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
