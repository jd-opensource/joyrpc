package io.joyrpc.config.inner;

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

import io.joyrpc.config.AbstractInterfaceOption;
import io.joyrpc.context.auth.IPPermission;
import io.joyrpc.context.limiter.LimiterConfiguration.ClassLimiter;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.WrapperParametric;
import io.joyrpc.invoker.CallbackMethod;
import io.joyrpc.permission.BlackWhiteList;
import io.joyrpc.permission.StringBlackWhiteList;

import javax.validation.Validator;
import java.util.Map;
import java.util.function.Supplier;

import static io.joyrpc.constants.Constants.*;
import static io.joyrpc.context.auth.IPPermissionConfiguration.IP_PERMISSION;
import static io.joyrpc.context.limiter.LimiterConfiguration.LIMITERS;

/**
 * 服务提供者接口选项
 */
public class InnerProviderOption extends AbstractInterfaceOption {

    /**
     * 方法黑白名单
     */
    protected BlackWhiteList<String> methodBlackWhiteList;
    /**
     * 接口IP限制
     */
    protected IntfConfiguration<String, IPPermission> ipPermissions;
    /**
     * 限流配置
     */
    protected IntfConfiguration<String, ClassLimiter> limiters;

    /**
     * 构造函数
     *
     * @param interfaceClass 接口类
     * @param interfaceName  接口名称
     * @param url            URL
     */
    public InnerProviderOption(final Class<?> interfaceClass, final String interfaceName, final URL url) {
        super(interfaceClass, interfaceName, url);
        setup();
        buildOptions();
    }

    @Override
    protected void setup() {
        super.setup();
        String include = url.getString(METHOD_INCLUDE_OPTION.getName());
        String exclude = url.getString(METHOD_EXCLUDE_OPTION.getName());
        this.methodBlackWhiteList = (include == null || include.isEmpty()) && (exclude == null || exclude.isEmpty()) ? null :
                new StringBlackWhiteList(include, exclude);
        this.ipPermissions = new IntfConfiguration<>(IP_PERMISSION, interfaceName);
        this.limiters = new IntfConfiguration<>(LIMITERS, interfaceName);
    }

    @Override
    protected void doClose() {
        super.doClose();
        ipPermissions.close();
        limiters.close();
    }

    @Override
    protected InnerMethodOption create(final WrapperParametric parametric) {
        return new InnerProviderMethodOption(
                getImplicits(parametric.getName()),
                parametric.getPositive(TIMEOUT_OPTION.getName(), timeout),
                new Concurrency(parametric.getInteger(CONCURRENCY_OPTION.getName(), concurrency)),
                getCachePolicy(parametric),
                getValidator(parametric),
                parametric.getString(HIDDEN_KEY_TOKEN, token),
                getCallback(parametric.getName()),
                methodBlackWhiteList,
                ipPermissions,
                limiters);
    }

    /**
     * 方法选项
     */
    protected static class InnerProviderMethodOption extends InnerMethodOption implements ProviderMethodOption {
        /**
         * 方法的黑白名单
         */
        protected BlackWhiteList<String> methodBlackWhiteList;
        /**
         * IP限制
         */
        protected Supplier<IPPermission> iPPermission;
        /**
         * 限流
         */
        protected Supplier<ClassLimiter> limiter;

        public InnerProviderMethodOption(final Map<String, ?> implicits, final int timeout, final Concurrency concurrency,
                                         final CachePolicy cachePolicy, final Validator validator,
                                         final String token, final CallbackMethod callback,
                                         final BlackWhiteList<String> methodBlackWhiteList,
                                         final Supplier<IPPermission> iPPermission,
                                         final Supplier<ClassLimiter> limiter) {
            super(implicits, timeout, concurrency, cachePolicy, validator, token, callback);
            this.methodBlackWhiteList = methodBlackWhiteList;
            this.iPPermission = iPPermission;
            this.limiter = limiter;
        }

        @Override
        public Map<String, ?> getImplicits() {
            return implicits;
        }


        @Override
        public BlackWhiteList<String> getMethodBlackWhiteList() {
            return methodBlackWhiteList;
        }


        @Override
        public IPPermission getIPPermission() {
            return iPPermission.get();
        }

        @Override
        public ClassLimiter getLimiter() {
            return limiter.get();
        }

    }

}
