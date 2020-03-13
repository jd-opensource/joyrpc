package io.joyrpc.cache;

import io.joyrpc.constants.Constants;
import io.joyrpc.exception.CacheException;
import io.joyrpc.expression.Expression;
import io.joyrpc.extension.Parametric;
import io.joyrpc.protocol.message.Invocation;
import io.joyrpc.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/**
 * 抽象的表达式缓存键生成器
 */
public abstract class AbstractExpressionCacheKeyGenerator implements CacheKeyGenerator.ExpressionGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AbstractExpressionCacheKeyGenerator.class);

    protected Expression expression;
    protected Parametric parametric;

    @Override
    public void setParametric(Parametric parameters) {
        this.parametric = parameters;
    }

    @Override
    public void setup() {
        if (parametric != null) {
            String el = StringUtils.trim(parametric.getString(Constants.CACHE_KEY_EXPRESSION));
            if (el != null && !el.isEmpty()) {
                try {
                    expression = create(el);
                } catch (Exception e) {
                    logger.error(String.format("Error occurs while build expression for %s", el), e);
                }
            }
        }
    }

    /**
     * 生成表达式
     *
     * @param el
     * @return
     */
    protected abstract Expression create(String el);

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
