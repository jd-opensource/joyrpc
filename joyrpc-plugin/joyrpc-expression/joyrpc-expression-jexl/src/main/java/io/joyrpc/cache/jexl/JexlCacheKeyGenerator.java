package io.joyrpc.cache.jexl;

import io.joyrpc.cache.AbstractExpressionCacheKeyGenerator;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;

import static io.joyrpc.cache.CacheKeyGenerator.JEXL_ORDER;

/**
 * JEXL3表达式缓存键生成器
 */
@Extension(value = "jexl", order = JEXL_ORDER)
@ConditionalOnClass({"org.apache.commons.jexl3.JexlBuilder"})
public class JexlCacheKeyGenerator extends AbstractExpressionCacheKeyGenerator {

    public JexlCacheKeyGenerator() {
        super("jexl");
    }

}