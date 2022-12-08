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
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

import static io.joyrpc.util.StringUtils.isEmpty;
import static io.joyrpc.util.StringUtils.split;

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
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String message = null;
        //没有配置插件
        if (isEmpty(value)) {
            if (!isEmpty(plugin.defaultValue())) {
                //有默认值
                value = plugin.defaultValue();
            } else if (!plugin.nullable()) {
                //不能使用优先级最高的插件
                message = String.format("plugin can not be empty.");
            } else {
                return true;
            }
        }
        //没有异常
        if (message == null) {
            if (extensionPoint == null) {
                message = String.format("No such extensionPoint %s in %s", plugin.name(), plugin.definition().getName());
            } else {
                String[] parts = plugin.multiple() ? split(value, ',') : new String[]{value};
                for (String part : parts) {
                    Object target = !plugin.multiple() && plugin.candidate() ? extensionPoint.getOrDefault(part) : extensionPoint.get(part);
                    if (target == null) {
                        message = String.format("No such extension '%s' of %s", part, plugin.extensible().getName());
                        break;
                    } else if (!plugin.extensible().isInstance(target)) {
                        message = String.format("%s is not a instance of %s", target.getClass().getName(), plugin.extensible().getName());
                        break;
                    }
                }
                if (message == null) {
                    return true;
                }
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
