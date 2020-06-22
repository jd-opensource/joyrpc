package io.joyrpc.cache.aviator;

import io.joyrpc.cache.AbstractExpressionCacheKeyGenerator;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;

import static io.joyrpc.cache.CacheKeyGenerator.AVIATOR_ORDER;

/**
 * aviator表达式缓存键生成器
 */
@Extension(value = "aviator", order = AVIATOR_ORDER)
@ConditionalOnClass({"com.googlecode.aviator.AviatorEvaluator"})
public class AviatorCacheKeyGenerator extends AbstractExpressionCacheKeyGenerator {

    public AviatorCacheKeyGenerator() {
        super("aviator");
    }

}