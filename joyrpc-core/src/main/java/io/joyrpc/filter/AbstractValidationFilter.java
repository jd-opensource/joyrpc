package io.joyrpc.filter;

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

import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.Converts;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.GENERIC_OPTION;
import static io.joyrpc.constants.Constants.VALIDATION_OPTION;

/**
 * 参数校验过滤器
 */
public class AbstractValidationFilter extends AbstractFilter {

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        //判断方法是否开启了验证
        Validator validator = request.getOption().getValidator();
        if (validator != null) {
            //JSR303验证
            Set<ConstraintViolation<Object>> violations = validator.forExecutables().validateParameters(
                    invocation.getObject(), invocation.getMethod(), invocation.getArgs());
            if (!violations.isEmpty()) {
                //有异常
                StringBuilder builder = new StringBuilder(100);
                int i = 0;
                for (ConstraintViolation<Object> violation : violations) {
                    if (i++ > 0) {
                        builder.append('\n');
                    }
                    builder.append("ConstraintViolation");
                    builder.append("{message=").append(violation.getMessage());
                    builder.append(", propertyPath=").append(violation.getPropertyPath());
                    builder.append(", class=").append(className);
                    builder.append('}');
                }
                // 无法直接序列化异常，只能转为字符串然后包装为RpcException
                RpcException re = new RpcException("validate is not passed, cause by: \n" + builder.toString(),
                        this instanceof ProviderFilter ? ExceptionCode.FILTER_VALID_PROVIDER : ExceptionCode.FILTER_VALID_CONSUMER);
                return CompletableFuture.completedFuture(new Result(request.getContext(), re));
            }
        }
        return invoker.invoke(request);
    }

    @Override
    public boolean test(final URL url) {
        //泛型调用不进行校验
        if (url.getBoolean(GENERIC_OPTION)) {
            return false;
        } else if (url.getBoolean(VALIDATION_OPTION)) {
            // 参数校验过滤器
            return true;
        }
        //判断是否设置了方法验证
        Map<String, String> map = url.endsWith("." + VALIDATION_OPTION.getName());
        for (String value : map.values()) {
            if (Converts.getBoolean(value, Boolean.FALSE)) {
                return true;
            }
        }
        return false;
    }

}
