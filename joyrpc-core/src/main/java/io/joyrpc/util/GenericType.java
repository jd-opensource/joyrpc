package io.joyrpc.util;

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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * 代表参数、返回值和异常的泛型信息
 */
public class GenericType {
    /**
     * 字段或参数的类型
     */
    protected Type type;
    /**
     * 单一变量
     */
    protected Variable variable;
    /**
     * 变量集合
     */
    protected Map<String, Variable> variables;

    /**
     * 构造函数
     *
     * @param type 类型
     */
    public GenericType(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    /**
     * 添加变量
     *
     * @param variable
     */
    protected void addVariable(final Variable variable) {
        if (variable == null) {
            return;
        }
        if (this.variable == null) {
            this.variable = variable;
        } else {
            if (variables == null) {
                variables = new HashMap<>();
                variables.put(this.variable.name, this.variable);
            }
            variables.put(variable.name, variable);
        }
    }

    /**
     * 获取变量
     *
     * @param name
     * @return
     */
    public Variable getVariable(final String name) {
        if (name == null) {
            return null;
        }
        if (variables == null) {
            return variable != null && variable.name.equals(name) ? variable : null;
        } else {
            return variables.get(name);
        }
    }

    /**
     * 计算泛型参数位置
     *
     * @param parameters
     */
    protected void compute(final Map<String, Integer> parameters) {
        if (variables != null) {
            variables.values().forEach(v -> compute(parameters, v));
        } else if (variable != null) {
            compute(parameters, variable);
        }
    }

    /**
     * 计算泛型参数位置
     *
     * @param parameters
     * @param variable
     */
    protected void compute(final Map<String, Integer> parameters, final Variable variable) {
        Integer pos = parameters.get(variable.name);
        if (pos != null) {
            variable.parameter = pos;
        }
    }

    /**
     * 泛型变量
     */
    public static class Variable {
        /**
         * 名称
         */
        protected String name;
        /**
         * 类型，可以是Class、ParameterizedType、GenericArrayType和Wi
         */
        protected Type type;
        /**
         * 第几个参数代表类型
         */
        protected int parameter = -1;

        public Variable(String name) {
            this.name = name;
        }

        public Variable(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public Variable(String name, Variable variable) {
            this.name = name;
            if (variable != null) {
                this.type = variable.type;
            }
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }

        public int getParameter() {
            return parameter;
        }

    }
}
