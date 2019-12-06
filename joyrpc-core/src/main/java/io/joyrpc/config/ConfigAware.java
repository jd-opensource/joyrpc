package io.joyrpc.config;

import java.util.concurrent.CompletableFuture;

/**
 * 感知配置
 */
public interface ConfigAware {

    /**
     * 构建
     *
     * @param config
     * @return
     */
    CompletableFuture<Void> setup(AbstractInterfaceConfig config);

}
