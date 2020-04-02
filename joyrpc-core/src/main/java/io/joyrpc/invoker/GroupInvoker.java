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
import io.joyrpc.InvokerAware;
import io.joyrpc.config.ConsumerConfig;
import io.joyrpc.extension.Extensible;
import io.joyrpc.extension.Prototype;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * 分组路由
 */
@Extensible("groupRoute")
public interface GroupInvoker extends Invoker, InvokerAware, Prototype {

    /**
     * 构建服务引用
     *
     * @return CompletableFuture
     */
    CompletableFuture<Void> refer();

    /**
     * 别名配置
     *
     * @param alias 分组
     */
    void setAlias(String alias);

    /**
     * 根据别名创建消费者配置的函数
     *
     * @param function 创建消费者的函数
     */
    void setConfigFunction(Function<String, ConsumerConfig<?>> function);

}
