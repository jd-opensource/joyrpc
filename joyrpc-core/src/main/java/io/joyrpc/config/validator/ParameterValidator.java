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

import io.joyrpc.context.RequestContext;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;

import static io.joyrpc.constants.Constants.INTERNAL_KEY_PREFIX;

/**
 * 参数验证
 */
public class ParameterValidator implements ConstraintValidator<ValidateParameter, Map<String, String>> {

    @Override
    public boolean isValid(final Map<String, String> value, final ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        String message = null;
        for (String key : value.keySet()) {
            if (key == null || key.isEmpty()) {
                message = "key can not be empty";
            } else if (RequestContext.INTERNAL_KEY.test(key)) {
                message = "key \'" + key + "\' can not start with \'" + INTERNAL_KEY_PREFIX + "\'";
                break;
            }
        }
        if (message == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    @Override
    public void initialize(final ValidateParameter plugin) {

    }
}
