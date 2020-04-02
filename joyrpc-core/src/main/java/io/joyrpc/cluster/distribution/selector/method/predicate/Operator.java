package io.joyrpc.cluster.distribution.selector.method.predicate;

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

import io.joyrpc.exception.RpcException;

import java.util.function.BiPredicate;

/**
 * @ClassName: Operators
 * @Description: 操作符
 * @date 2019年3月4日 下午6:39:52
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public enum Operator {

    nil("is null", (left, right) -> left == null), // is null
    eq("==", (left, right) -> left.equals(right)), // ==
    neq("!=", (left, right) -> !left.equals(right)), // !=
    gteq(">=", (left, right) -> ((Comparable) left).compareTo(right) >= 0), // >=
    lteq("<=", (left, right) -> ((Comparable) left).compareTo(right) <= 0),// <=
    gt(">", (left, right) -> ((Comparable) left).compareTo(right) > 0),// >
    lt("<", (left, right) -> ((Comparable) left).compareTo(right) < 0);// <

    Operator(String value, BiPredicate<Object, Object> match) {
        this.value = value;
        this.match = match;
    }

    private String value;

    private BiPredicate<Object, Object> match;

    public static Operator of(String value) {
        for (Operator oper : Operator.values()) {
            if (oper.value.equals(value)) {
                return oper;
            }
        }
        return null;
    }

    public String get() {
        return this.value;
    }

    public boolean match(Object left, Object right) {
        if (valid(left, right)) {
            return this.match.test(left, right);
        } else {
            throw new RpcException("Unsupported relational operator [" + this.toString() + "] of " + left + " and " + right);
        }
    }

    private boolean valid(final Object left, final Object right) {

        if (isObject() && (left == null || right == null)) {
            return false;
        }

        if (isComparable() && (!(left instanceof Comparable) || !(right instanceof Comparable))) {
            return false;
        }

        return true;
    }

    private boolean isComparable() {
        return this == gteq || this == lteq || this == gt || this == lt;
    }

    private boolean isObject() {
        return this == eq || this == neq || isComparable();
    }
}
