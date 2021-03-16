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
import io.joyrpc.InvokerAware;
import io.joyrpc.Result;
import io.joyrpc.option.InterfaceOption;
import io.joyrpc.extension.Prototype;
import io.joyrpc.extension.URL;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.protocol.message.RequestMessage;

import java.util.concurrent.CompletableFuture;

/**
 * 过滤器接口，由于要设置URL，插件都需要配置成多例
 */
@FunctionalInterface
public interface Filter extends InvokerAware, Prototype {
    /**
     * 自定义位
     */
    int CUSTOM = 0;
    /**
     * 系统位
     */
    int SYSTEM = 1;
    /**
     * 全局位
     */
    int GLOBAL = 2;
    /**
     * 系统全局
     */
    int SYSTEM_GLOBAL = 3;
    /**
     * 系统内置不能删除
     */
    int INNER = 7;

    /**
     * 跟踪顺序
     */
    int TRACE_ORDER = -65535;

    /**
     * 调用过滤链，返回值对Result，便于线程变量切换恢复
     *
     * @param invoker 调用器
     * @param request 请求
     * @return
     */
    CompletableFuture<Result> invoke(Invoker invoker, RequestMessage<Invocation> request);

    /**
     * 关闭，用于资源释放
     *
     * @return
     */
    default CompletableFuture<Void> close() {
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 判断是否要开启
     *
     * @param url url
     * @return 开启标识
     */
    default boolean test(URL url) {
        return true;
    }

    /**
     * 判断是否要开启
     *
     * @param option 选项
     * @return 开启标识
     */
    default boolean test(InterfaceOption option) {
        return false;
    }

    /**
     * 获取URL
     *
     * @return
     */
    default URL getUrl() {
        return null;
    }

    /**
     * 类型
     *
     * @return
     */
    default int type() {
        return CUSTOM;
    }

}
