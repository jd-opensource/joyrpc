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

import io.joyrpc.GenericService;
import io.joyrpc.Invoker;
import io.joyrpc.Result;
import io.joyrpc.constants.ExceptionCode;
import io.joyrpc.exception.RpcException;
import io.joyrpc.extension.Converts;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;
import io.joyrpc.util.MethodOption.NameKeyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.MethodDescriptor;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static io.joyrpc.constants.Constants.GENERIC_OPTION;
import static io.joyrpc.constants.Constants.VALIDATION_OPTION;

/**
 * 参数校验过滤器
 *
 * @description: 支持接口级或者方法级配置，服务端和客户端都可以配置，需要引入第三方jar包<br>
 */
public class AbstractValidationFilter extends AbstractFilter {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractValidationFilter.class);
    /**
     * 验证器
     */
    protected Validator validator;
    //方法验证
    protected NameKeyOption<Boolean> validations;

    @Override
    public void setup() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        //默认是否认证
        boolean validate = url.getBoolean(VALIDATION_OPTION.getName());
        //直接传入默认值，加快构建速度
        if (validator == null || GenericService.class.equals(clazz)) {
            validations = null;
        } else {
            //得到类的验证描述
            BeanDescriptor beanDescriptor = validator.getConstraintsForClass(clazz);
            //遍历方法，判断是否需要验证
            validations = new NameKeyOption<>(clazz, o -> {
                //判断该方法的配置是否开启了验证
                if (!url.getBoolean(getOption(o.getName(), VALIDATION_OPTION.getName(), validate))) {
                    return false;
                }
                //判断该方法上是有有验证注解
                MethodDescriptor descriptor = beanDescriptor.getConstraintsForMethod(o.getName(), o.getParameterTypes());
                return descriptor == null ? false : descriptor.hasConstrainedParameters();
            });
        }
    }

    @Override
    public CompletableFuture<Result> invoke(final Invoker invoker, final RequestMessage<Invocation> request) {
        Invocation invocation = request.getPayLoad();
        //过滤掉泛化调用
        if (!invocation.isGeneric() && validations != null) {
            //判断方法是否开启了验证
            Boolean validate = validations.get(invocation.getMethodName());
            if (validate != null && validate) {
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
