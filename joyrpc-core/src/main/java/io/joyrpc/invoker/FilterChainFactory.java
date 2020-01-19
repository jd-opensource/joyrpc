package io.joyrpc.invoker;

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
import io.joyrpc.config.AbstractInterfaceConfig;
import io.joyrpc.extension.Extensible;
import io.joyrpc.filter.Filter;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import javax.validation.ConstraintValidatorContext;
import java.util.concurrent.CompletableFuture;

/**
 * 构造处理链
 */
@Extensible("filterChainFactory")
public interface FilterChainFactory {

    /**
     * 构造消费者过滤链
     *
     * @param refer 服务消费者
     * @param last  最后执行逻辑
     * @return 处理链
     */
    Invoker build(Refer refer, Invoker last);

    /**
     * 构造服务提供者过滤链
     *
     * @param exporter 服务提供者
     * @param last     最后执行逻辑
     * @return 处理链
     */
    Invoker build(Exporter exporter, Invoker last);

    /**
     * 验证配置的过滤器插件是否存在
     *
     * @param config
     * @param context
     * @return
     */
    boolean validFilters(final AbstractInterfaceConfig config, final ConstraintValidatorContext context);
}
