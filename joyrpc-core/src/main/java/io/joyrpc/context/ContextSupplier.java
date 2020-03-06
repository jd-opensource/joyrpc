package io.joyrpc.context;

/**
 * 全局上下文提供者
 */
public interface ContextSupplier {

    int CONTEXT_ORDER = 100;

    int SPRING_ORDER = CONTEXT_ORDER + 10;

    /**
     * 识别上下文
     *
     * @param key
     * @return
     */
    Object recognize(String key);

}
