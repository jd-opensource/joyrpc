package io.joyrpc.expression.spring;

/*-
 * #%L
 * joyrpc
 * %%
 * Copyright (C) 2019 joyrpc.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.joyrpc.expression.Expression;
import io.joyrpc.expression.ExpressionProvider;
import io.joyrpc.extension.Extension;
import io.joyrpc.extension.condition.ConditionalOnClass;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * Spring expression language provider
 */
@Extension(value = "spel", order = ExpressionProvider.SPEL_ORDER)
@ConditionalOnClass({"org.springframework.expression.Expression"})
public class SpelProvider implements ExpressionProvider {

    protected ExpressionParser parser = new SpelExpressionParser();

    @Override
    public Expression build(final String expression) {
        return expression == null ? null : new SpringExpression(parser.parseExpression(expression));
    }

    /**
     * Spring表达式
     */
    protected static class SpringExpression implements Expression {

        protected org.springframework.expression.Expression expression;

        public SpringExpression(org.springframework.expression.Expression expression) {
            this.expression = expression;
        }

        @Override
        public Object evaluate(final Map<String, Object> context) {
            if (context == null || expression == null) {
                return null;
            }
            StandardEvaluationContext sec = new StandardEvaluationContext(context);
            sec.addPropertyAccessor(new MapAccessor());
            return expression.getValue(sec);
        }
    }
}
