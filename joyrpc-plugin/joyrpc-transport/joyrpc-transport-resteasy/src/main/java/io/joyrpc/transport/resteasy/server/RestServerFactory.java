package io.joyrpc.transport.resteasy.server;

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
import io.joyrpc.transport.Client;
import io.joyrpc.transport.EndpointFactory;
import io.joyrpc.transport.Server;
import io.joyrpc.transport.transport.ClientTransport;
import io.joyrpc.transport.transport.TransportFactory;

import java.util.function.Function;

import static io.joyrpc.Plugin.TRANSPORT_FACTORY;
import static io.joyrpc.constants.Constants.TRANSPORT_FACTORY_OPTION;

/**
 * RestEasy服务工厂类
 */
@Extension(value = "resteasy", order = 10)
public class RestServerFactory implements EndpointFactory {

    @Override
    public Client createClient(URL url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Client createClient(URL url, Function<ClientTransport, Client> function) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Server createServer(URL url) {
        TransportFactory factory = TRANSPORT_FACTORY.getOrDefault(url.getString(TRANSPORT_FACTORY_OPTION));
        return new RestServer(url, factory);
    }
}
