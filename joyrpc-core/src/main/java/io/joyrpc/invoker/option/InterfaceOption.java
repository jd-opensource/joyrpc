package io.joyrpc.invoker.option;

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

/**
 * 接口运行时选项，抽取出接口是方便第三方在扩展Filter的时候也可以扩展实现方法选项，用于提前绑定相关参数
 */
public interface InterfaceOption {

    /**
     * 根据方法名称返回选项
     *
     * @param methodName 方法名称
     * @return 选项
     */
    MethodOption getOption(String methodName);

    /**
     * 是否泛化调用
     *
     * @return 泛化调用标识
     */
    boolean isGeneric();

    /**
     * 是否启用假数据
     *
     * @return 启用假数据标识
     */
    boolean isMock();

    /**
     * 是否有回调函数
     *
     * @return 回调函数标识
     */
    boolean isCallback();

    /**
     * 是否启动跟踪
     *
     * @return 启动跟踪标识
     */
    boolean isTrace();

    /**
     * 是否启用缓存
     *
     * @return 启用缓存标识
     */
    boolean isCache();

    /**
     * 是否启用认证
     *
     * @return 启用认证标识
     */
    boolean isValidation();

    /**
     * 是否启用并发数限制
     *
     * @return 启用并发数限制标识
     */
    boolean isConcurrency();

    /**
     * 是否启用限流
     *
     * @return 启用限流标识
     */
    boolean isLimiter();

    /**
     * 是否启用方法黑白名单
     *
     * @return 启用方法黑白名单标识
     */
    default boolean isMethodBlackWhiteList() {
        return false;
    }

    /**
     * 关闭，释放资源，例如移除监听器
     */
    default void close() {
    }

}
