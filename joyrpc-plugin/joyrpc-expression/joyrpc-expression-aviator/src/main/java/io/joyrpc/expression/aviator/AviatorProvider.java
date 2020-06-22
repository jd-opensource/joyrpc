package io.joyrpc.expression.aviator;

import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.AviatorEvaluatorInstance;
import io.joyrpc.expression.Expression;
import io.joyrpc.expression.ExpressionProvider;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;

import java.util.Map;

import static io.joyrpc.expression.ExpressionProvider.JEXL_ORDER;

/**
 * JEXL3表达式引擎提供者
 */
@Extension(value = "aviator", order = JEXL_ORDER)
@ConditionalOnClass("com.googlecode.aviator.AviatorEvaluator")
public class AviatorProvider implements ExpressionProvider {

    /**
     * 引擎
     */
    protected AviatorEvaluatorInstance engine;

    public AviatorProvider() {
        engine = AviatorEvaluator.getInstance();
    }

    @Override
    public Expression build(final String expression) {
        return expression == null ? null : new AviatorExpression(engine.compile(expression));
    }

    /**
     * 表达式对象
     */
    protected static class AviatorExpression implements Expression {
        /**
         * 表达式缓存
         */
        private com.googlecode.aviator.Expression expression;

        public AviatorExpression(com.googlecode.aviator.Expression expression) {
            this.expression = expression;
        }

        @Override
        public Object evaluate(final Map<String, Object> context) {
            return expression.execute(context);
        }
    }
}
