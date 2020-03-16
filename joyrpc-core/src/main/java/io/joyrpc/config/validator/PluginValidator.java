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

import io.joyrpc.extension.ExtensionPoint;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

import static io.joyrpc.util.StringUtils.isEmpty;

/**
 * 插件验证
 */
public class PluginValidator implements ConstraintValidator<ValidatePlugin, String> {
    /**
     * 扩展点
     */
    protected ExtensionPoint<?, String> extensionPoint;
    /**
     * 插件注解
     */
    protected ValidatePlugin plugin;

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        String v = value;
        if (isEmpty(v)) {
            if (!isEmpty(plugin.defaultValue())) {
                v = plugin.defaultValue();
            } else {
                return true;
            }
        }
        String message;
        if (extensionPoint == null) {
            message = String.format("No such extensionPoint %s in %s", plugin.name(), plugin.definition().getName());
        } else {
            Object target = plugin.candidate() ? extensionPoint.getOrDefault(v) : extensionPoint.get(v);
            if (target == null) {
                message = String.format("No such extension '%s' of %s", v, plugin.extensible().getName());
            } else if (!plugin.extensible().isInstance(target)) {
                message = String.format("%s is not a instance of %s", target.getClass().getName(), plugin.extensible().getName());
            } else {
                return true;
            }
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    @Override
    public void initialize(final ValidatePlugin plugin) {
        this.plugin = plugin;
        try {
            Field field = plugin.definition().getField(plugin.name());
            extensionPoint = (ExtensionPoint<?, String>) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
        }
    }
}
