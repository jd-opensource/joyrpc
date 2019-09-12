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
import io.joyrpc.transport.transport.TransportFactory;

import static io.joyrpc.Plugin.TRANSPORT_FACTORY;
import static io.joyrpc.constants.Constants.TRANSPORT_FACTORY_OPTION;

/**
 * @date: 2019/3/26
 */
@Extension(value = "default", order = 1)
public class DefaultEndpointFactory implements EndpointFactory {

    @Override
    public Client createClient(final URL url) {
        if (url == null) {
            return null;
        }
        TransportFactory factory = getTransportFactory(url);
        return factory == null ? null : new DecoratorClient(url, factory.createClientTransport(url));
    }

    @Override
    public Server createServer(final URL url) {
        if (url == null) {
            return null;
        }
        TransportFactory factory = getTransportFactory(url);
        return factory == null ? null : new DecoratorServer(url, factory.createServerTransport(url));
    }

    /**
     * 获取通道工厂类
     *
     * @param url
     * @return
     */
    protected TransportFactory getTransportFactory(final URL url) {
        return TRANSPORT_FACTORY.getOrDefault(url.getString(TRANSPORT_FACTORY_OPTION));
    }
}
