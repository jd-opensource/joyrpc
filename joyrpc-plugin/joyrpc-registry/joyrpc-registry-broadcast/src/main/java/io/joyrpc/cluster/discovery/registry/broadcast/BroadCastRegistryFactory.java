package io.joyrpc.cluster.discovery.registry.broadcast;

import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.registry.AbstractRegistryFactory;
import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;

/**
 * hazelcast注册中心实现插件
 */
@Extension(value = "broadcast")
public class BroadCastRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected Registry createRegistry(String name, URL url, Backup backup) {
        return new BroadCastRegistry(name, url, backup);
    }
}
