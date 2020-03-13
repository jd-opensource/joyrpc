package io.joyrpc.cache.jexl;

import io.joyrpc.cache.AbstractExpressionCacheKeyGenerator;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;

/**
 * JEXL3表达式缓存键生成器
 */
@Extension("jexl")
@ConditionalOnClass({"org.apache.commons.jexl3.JexlEngine"})
public class JexlCacheKeyGenerator extends AbstractExpressionCacheKeyGenerator {

    public JexlCacheKeyGenerator() {
        super("jexl");
    }

}