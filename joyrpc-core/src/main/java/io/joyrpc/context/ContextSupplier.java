package io.joyrpc.context;

/**
 * 全局上下文提供者
 */
public interface ContextSupplier {

    /**
     * 识别上下文
     *
     * @param key
     * @return
     */
    Object recognize(String key);

}
