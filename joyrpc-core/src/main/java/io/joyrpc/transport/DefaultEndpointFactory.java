package io.joyrpc.transport;

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

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;

import java.util.function.Function;

import static io.joyrpc.Plugin.TRANSPORT_FACTORY;
import static io.joyrpc.constants.Constants.TRANSPORT_FACTORY_OPTION;

/**
 * 默认端点工厂类
 */
@Extension(value = "default", order = 1)
public class DefaultEndpointFactory implements EndpointFactory {

    @Override
    public Client createClient(final URL url) {
        return create(url, factory -> new DecoratorClient(url, factory.createClient(url)));
    }

    @Override
    public Client createClient(URL url, Function<TransportClient, Client> function) {
        return create(url, factory -> function == null ? new DecoratorClient(url, factory.createClient(url)) :
                function.apply(factory.createClient(url)));
    }

    @Override
    public Server createServer(final URL url) {
        return create(url, factory -> new DecoratorServer(url, factory.createServer(url)));
    }

    /**
     * 构建
     *
     * @param url      url
     * @param function 函数
     * @param <M>
     * @return
     */
    protected <M> M create(final URL url, final Function<TransportFactory, M> function) {
        if (url != null) {
            TransportFactory factory = TRANSPORT_FACTORY.getOrDefault(url.getString(TRANSPORT_FACTORY_OPTION));
            if (factory != null) {
                return function.apply(factory);
            }
        }
        return null;

    }
}
