package io.joyrpc.cache.expression;

import io.joyrpc.Plugin;
import io.joyrpc.cache.CacheKeyGenerator;
import io.joyrpc.constants.Constants;
import io.joyrpc.exception.CacheException;
import io.joyrpc.expression.Expression;
import io.joyrpc.expression.ExpressionProvider;
import io.joyrpc.extension.Extension;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 表达式
 */
@Extension(value = "expression", singleton = false)
public class ExpressionCacheKeyGenerator implements CacheKeyGenerator.ExpressionGenerator {

    private static final Logger logger = LoggerFactory.getLogger(ExpressionCacheKeyGenerator.class);

    protected Expression expression;
    protected BiFunction<String, String, String> parameters;

    @Override
    public void setParameters(final BiFunction<String, String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void setup() {
        ExpressionProvider provider = Plugin.EXPRESSION_PROVIDER.get();
        if (provider != null && parameters != null) {
            String el = StringUtils.trim(parameters.apply(Constants.CACHE_KEY_EXPRESSION, null));
            if (el != null && !el.isEmpty()) {
                try {
                    expression = provider.build(el);
                } catch (Exception e) {
                    logger.error(String.format("Error occurs while build expression for %s", el), e);
                }
            }
        }
    }

    @Override
    public Object generate(final Invocation invocation) throws CacheException {
        Object[] args = invocation.getArgs();
        if (expression == null) {
            //无参
            if (args == null || args.length == 0) {
                return "";
            } else {
                return null;
            }
        }
        Method method = invocation.getMethod();
        Map<String, Object> context = new HashMap<>();
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            context.put(parameters[i].getName(), args[i]);
        }
        context.put("args", args);
        try {
            return expression.evaluate(context);
        } catch (Exception e) {
            throw new CacheException(String.format("Error occurs while generate cache key for %s.%s", invocation.getClassName(), method.getName()));
        }
    }
}
