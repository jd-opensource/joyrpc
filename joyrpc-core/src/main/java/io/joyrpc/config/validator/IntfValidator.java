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
import io.joyrpc.config.ProviderConfig;
import io.joyrpc.constants.Constants;
import io.joyrpc.util.Pair;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.ValidationException;

import static io.joyrpc.Plugin.INTERFACE_VALIDATOR;
import static io.joyrpc.util.ClassUtils.forName;
import static io.joyrpc.util.StringUtils.isEmpty;

/**
 * 接口验证
 */
public class IntfValidator implements ConstraintValidator<ValidateInterface, AbstractInterfaceConfig> {

    @Override
    public boolean isValid(final AbstractInterfaceConfig config, final ConstraintValidatorContext context) {

        Pair<String, String> error = null;
        String interfaceClazz = config.getInterfaceClazz();
        if (interfaceClazz == null || interfaceClazz.isEmpty()) {
            error = new Pair<>("interfaceClazz", "interfaceClazz can not be empty");
        } else {
            Class<?> clazz = config.getProxyClass();
            if (clazz == null) {
                try {
                    clazz = forName(interfaceClazz);
                    config.setInterfaceClass(clazz);
                } catch (ClassNotFoundException e) {
                    error = new Pair<>("interfaceClazz", "class is not found. '" + interfaceClazz + "'");
                }
            }
            if (error == null) {
                if (!clazz.isInterface()) {
                    error = new Pair<>("interfaceClass", "class is not a interface. '" + clazz.getName() + "'");
                } else if (config instanceof ProviderConfig) {
                    error = valid((ProviderConfig) config, clazz);
                }
            }

        }
        if (error != null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(error.getValue())
                    .addPropertyNode(error.getKey())
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    /**
     * 验证服务提供者接口
     *
     * @param config
     * @param clazz
     * @return
     */
    protected Pair<String, String> valid(final ProviderConfig config, final Class clazz) {
        Object ref = config.getRef();
        if (ref != null && !clazz.isInstance(ref)) {
            return new Pair<>("interfaceClass", String.format("%s is not an instance of %s", ref.getClass().getName(), clazz.getName()));
        } else if (config.getEnableValidator() == null || !config.getEnableValidator()) {
            return null;
        } else {
            String validatorName = isEmpty(config.getInterfaceValidator()) ?
                    Constants.INTERFACE_VALIDATOR_OPTION.get() :
                    config.getInterfaceValidator();
            InterfaceValidator validator = INTERFACE_VALIDATOR.get(validatorName);
            if (validator == null) {
                return new Pair<>("interfaceValidator",
                        String.format("No such extension \'%s\' of %s", validatorName, InterfaceValidator.class.getName()));
            } else {
                try {
                    validator.validate(clazz);
                    return null;
                } catch (ValidationException e) {
                    return new Pair<>("interfaceClass", e.getMessage());
                }
            }
        }
    }

    @Override
    public void initialize(final ValidateInterface validate) {
    }
}
