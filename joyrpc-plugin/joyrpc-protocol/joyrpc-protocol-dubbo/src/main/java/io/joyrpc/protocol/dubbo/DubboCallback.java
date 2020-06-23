package io.joyrpc.protocol.dubbo;

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

import io.joyrpc.extension.URL;
import io.joyrpc.invoker.CallbackInvoker;
import io.joyrpc.invoker.ServiceManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dubbo callback兼容处理类
 */
public class DubboCallback {

    protected static final Map<String, String> callbackInsIds = new ConcurrentHashMap<>();

    /**
     * 将joyrpc的callbackInsId转换为dubbo的callbackInsId
     *
     * @param callbackInsId joyrpc的callbackInsId
     * @return
     */
    public static String toDubboInsId(Object callbackInsId) {
        if (callbackInsId == null) {
            return "0";
        }
        CallbackInvoker invoker = ServiceManager.getConsumerCallback().getInvoker(callbackInsId.toString());
        if (invoker != null) {
            String dubboId = String.valueOf(System.identityHashCode(invoker));
            callbackInsIds.put(dubboId, callbackInsId.toString());
            return dubboId;
        }
        return "0";
    }

    /**
     * 将dubbo的callbackInsId转换为joyrpc的callbackInsId
     *
     * @param callbackInsId dubbo的callbackInsId
     * @return
     */
    public static String toJoyInsId(Object callbackInsId) {
        return callbackInsId == null ? "" : callbackInsIds.get(callbackInsId.toString());
    }

    /**
     * 根据 joyrpc 的 callbackInsId 获取provider端serviceURL
     *
     * @param callbackInsId
     * @return
     */
    public static URL getProducerServiceUrl(Object callbackInsId) {
        if (callbackInsId == null) {
            return null;
        }
        CallbackInvoker invoker = ServiceManager.getProducerCallback().getInvoker(callbackInsId.toString());
        return invoker == null ? null : invoker.getTransport().getUrl();
    }
}
