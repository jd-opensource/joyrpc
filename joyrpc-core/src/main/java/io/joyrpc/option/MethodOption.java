package io.joyrpc.option;

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

import io.joyrpc.invoker.CallbackMethod;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.transaction.TransactionOption;
import io.joyrpc.util.GenericMethod;

import javax.validation.Validator;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 方法选项
 */
public interface MethodOption {

    /**
     * 获取方法
     *
     * @return 方法
     */
    Method getMethod();

    /**
     * 获取泛型信息
     *
     * @return 泛型方法
     */
    GenericMethod getGenericMethod();

    /**
     * 方法级别隐式传参，合并了接口的隐藏参数
     *
     * @return 合并了接口的方法级隐式传参数
     */
    Map<String, ?> getImplicits();

    /**
     * 获取超时时间
     *
     * @return 超时时间
     */
    int getTimeout();

    /**
     * 并发配置
     *
     * @return 并发配置
     */
    Concurrency getConcurrency();

    /**
     * 缓存策略
     *
     * @return 缓存策略
     */
    CacheOption getCachePolicy();

    /**
     * 参数验证
     *
     * @return 参数验证
     */
    Validator getValidator();

    /**
     * 令牌
     *
     * @return 令牌
     */
    String getToken();

    /**
     * 获取回调方法信息
     *
     * @return 回调方法
     */
    CallbackMethod getCallback();

    /**
     * 获取事务选项
     *
     * @return 事务选项
     */
    TransactionOption getTransactionOption();

    /**
     * 是否异步调用
     *
     * @return 异步调用标识
     */
    boolean isAsync();

    /**
     * 获取参数信息
     *
     * @return 参数信息
     */
    ArgumentOption getArgType();

    /**
     * 获取参数描述信息
     *
     * @return 参数描述信息
     */
    String getDescription();

    /**
     * 是否开启跟踪
     *
     * @return 开启跟踪标识
     */
    boolean isTrace();

    /**
     * 获取跟踪的span名称
     *
     * @param invocation 调用
     * @return
     */
    String getTraceSpanId(Invocation invocation);
}
