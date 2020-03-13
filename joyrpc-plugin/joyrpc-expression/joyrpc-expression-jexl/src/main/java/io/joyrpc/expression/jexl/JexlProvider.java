package io.joyrpc.expression.jexl;

import io.joyrpc.expression.Expression;
import io.joyrpc.expression.ExpressionProvider;
import io.joyrpc.extension.Extension;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import java.util.Map;

/**
 * JEXL3表达式引擎提供者
 */
@Extension("jexl")
public class JexlProvider implements ExpressionProvider {

    /**
     * 引擎
     */
    protected JexlEngine engine;

    public JexlProvider() {
        engine = new JexlBuilder().create();
    }

    @Override
    public Expression build(final String expression) {
        return expression == null ? null : new Jexl3Expression(engine.createExpression(expression));
    }

    /**
     * 表达式对象
     */
    protected static class Jexl3Expression implements Expression {
        /**
         * 表达式缓存
         */
        private JexlExpression expression;

        public Jexl3Expression(JexlExpression expression) {
            this.expression = expression;
        }

        @Override
        public Object evaluate(final Map<String, Object> context) {
            return expression.evaluate(new MapContext(context));
        }
    }
}
