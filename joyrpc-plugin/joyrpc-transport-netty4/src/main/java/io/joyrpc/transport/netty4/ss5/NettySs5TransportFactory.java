package io.joyrpc.transport.netty4.ss5;

import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;
import io.joyrpc.extension.condition.ConditionalOnClass;
import io.joyrpc.transport.netty4.transport.NettyTransportFactory;
import io.joyrpc.transport.transport.ClientTransport;

@Extension("netty4.ss5")
@ConditionalOnClass({"io.netty.channel.Channel", "io.netty.handler.proxy.Socks5ProxyHandler"})
public class NettySs5TransportFactory extends NettyTransportFactory {

    @Override
    public ClientTransport createClientTransport(final URL url) {
        return new NettySs5ClientTransport(url);
    }

}
