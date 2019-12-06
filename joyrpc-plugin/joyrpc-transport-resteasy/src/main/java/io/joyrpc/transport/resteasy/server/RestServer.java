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

import io.joyrpc.config.AbstractInterfaceConfig;
import io.joyrpc.config.ConfigAware;
import io.joyrpc.config.ProviderConfig;
import io.joyrpc.event.AsyncResult;
import io.joyrpc.exception.InitializationException;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.URLOption;
import io.joyrpc.transport.DecoratorServer;
import io.joyrpc.transport.channel.Channel;
import io.joyrpc.transport.channel.ChannelHandlerChain;
import io.joyrpc.transport.codec.Codec;
import io.joyrpc.transport.codec.ProtocolAdapter;
import io.joyrpc.transport.resteasy.codec.ResteasyCodec;
import io.joyrpc.transport.resteasy.mapper.ApplicationExceptionMapper;
import io.joyrpc.transport.resteasy.mapper.ClientErrorExceptionMapper;
import io.joyrpc.transport.resteasy.mapper.IllegalArgumentExceptionMapper;
import io.joyrpc.transport.transport.ServerTransport;
import io.joyrpc.util.Futures;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.netty.RequestDispatcher;
import org.jboss.resteasy.plugins.server.resourcefactory.SingletonResource;
import org.jboss.resteasy.spi.ApplicationException;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GetRestful;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static io.joyrpc.transport.Endpoint.Status.CLOSED;

/**
 * Rest服务
 */
public class RestServer extends DecoratorServer<ServerTransport> implements ConfigAware {

    protected static final URLOption<String> REST_ROOT = new URLOption<>("restRoot", "/");

    protected Codec codec;

    protected String root;

    protected ResteasyDeployment deployment;

    protected RequestDispatcher dispatcher;

    public RestServer(URL url, ServerTransport transport) {
        super(url, transport);
        this.deployment = new ResteasyDeployment();
        this.root = url.getString(REST_ROOT);
        if (REST_ROOT.getValue().equals(root)) {
            this.root = "";
        }
    }

    @Override
    public void open(final Consumer<AsyncResult<Channel>> consumer) {
        Status status = getStatus();
        if (status == CLOSED) {
            deployment.start();
            ResteasyProviderFactory providerFactory = deployment.getProviderFactory();
            Map<Class<?>, ExceptionMapper> mapperMap = providerFactory.getExceptionMappers();
            mapperMap.put(ApplicationException.class, ApplicationExceptionMapper.mapper);
            mapperMap.put(ClientErrorException.class, ClientErrorExceptionMapper.mapper);
            mapperMap.put(IllegalArgumentException.class, IllegalArgumentExceptionMapper.mapper);
            this.dispatcher = new RequestDispatcher((SynchronousDispatcher) deployment.getDispatcher(), providerFactory, null);
            this.codec = new ResteasyCodec(root, dispatcher);
            this.transport.setCodec(codec);
        }
        super.open(r -> {
            if (!r.isSuccess()) {
                deployment.stop();
            }
        });
    }

    @Override
    public void close(Consumer consumer) {
        deployment.stop();
        super.close(consumer);
    }

    @Override
    public void setChannelHandlerChain(ChannelHandlerChain chain) {
    }

    @Override
    public void setCodec(Codec codec) {
    }

    @Override
    public void setAdapter(ProtocolAdapter adapter) {
    }

    @Override
    public CompletableFuture<Void> setup(final AbstractInterfaceConfig config) {
        Object ref = ((ProviderConfig) config).getRef();
        Class restful = GetRestful.getRootResourceClass(ref.getClass());
        if (restful != null) {
            restful = GetRestful.getRootResourceClass(ref.getClass());
            deployment.getRegistry().addResourceFactory(new SingletonResource(ref), "/", restful);
            return CompletableFuture.completedFuture(null);
        } else {
            restful = GetRestful.getRootResourceClass(config.getInterfaceClass());
            if (restful == null) {
                return Futures.completeExceptionally(new InitializationException(
                        "there is not any @Path in " + config.getInterfaceClazz()));
            } else {
                deployment.getRegistry().addSingletonResource(ref, "/");
                return CompletableFuture.completedFuture(null);
            }
        }
    }


    public ResteasyDeployment getDeployment() {
        return deployment;
    }

    public RequestDispatcher getDispatcher() {
        return dispatcher;
    }
}
