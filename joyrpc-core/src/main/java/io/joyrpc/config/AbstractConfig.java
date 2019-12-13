package io.joyrpc.config;

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

import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.IllegalConfigureException;
import io.joyrpc.extension.URLBiOption;
import io.joyrpc.extension.URLOption;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 配置基类
 */
public abstract class AbstractConfig {

    protected void addElement2Map(final Map<String, String> dest, final String key, final Object value) {
        if (null != value) {
            String v = value.toString();
            if (v != null && !v.isEmpty()) {
                dest.put(key, value.toString());
            }
        }
    }

    protected void addElement2Map(final Map<String, String> dest, final URLOption option, final Object value) {
        addElement2Map(dest, option.getName(), value);
    }

    protected void addElement2Map(final Map<String, String> dest, final URLBiOption option, final Object value) {
        addElement2Map(dest, option.getName(), value);
    }

    protected Map<String, String> addAttribute2Map(Map<String, String> params) {
        return params;
    }

    public AbstractConfig() {
    }

    public AbstractConfig(AbstractConfig config) {

    }

    protected Map<String, String> addAttribute2Map() {
        Map<String, String> result = new HashMap<>();
        addAttribute2Map(result);
        return result;
    }

    protected String name() {
        return "";
    }

    /**
     * 验证
     */
    public void validate() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        if (validator == null) {
            throw new IllegalConfigureException("javax.validation.Validator is not implement", ExceptionCode.COMMON_VALUE_ILLEGAL);
        } else {
            //JSR303验证
            Set<ConstraintViolation<Object>> violations = validator.validate(this);
            if (!violations.isEmpty()) {
                //有异常
                StringBuilder builder = new StringBuilder(100).append(name());
                for (ConstraintViolation<Object> violation : violations) {
                    builder.append("\n\t");
                    builder.append("ConstraintViolation");
                    builder.append("{message=\"").append(violation.getMessage());
                    builder.append("\", propertyPath=").append(violation.getPropertyPath());
                    builder.append('}');
                }
                throw new IllegalConfigureException(builder.toString(), ExceptionCode.COMMON_VALUE_ILLEGAL);
            }
        }
    }

}
