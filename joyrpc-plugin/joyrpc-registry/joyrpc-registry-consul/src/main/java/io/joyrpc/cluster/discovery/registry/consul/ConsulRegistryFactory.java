package io.joyrpc.cluster.discovery.registry.consul;

import io.joyrpc.cluster.discovery.backup.Backup;
import io.joyrpc.cluster.discovery.registry.AbstractRegistryFactory;
import io.joyrpc.cluster.discovery.registry.Registry;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.URL;

@Extension(value = "consul")
public class ConsulRegistryFactory extends AbstractRegistryFactory {

    @Override
    protected Registry createRegistry(String name, URL url, Backup backup) {
        return new ConsulRegistry(name, url, backup);
    }
}
