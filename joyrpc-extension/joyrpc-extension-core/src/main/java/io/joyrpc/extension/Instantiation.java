package io.joyrpc.extension;

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

/**
 * 实例化接口
 */
@FunctionalInterface
public interface Instantiation {

    /**
     * 构建实例
     *
     * @param name 实例名称
     * @param <T>
     * @return
     */
    <T, M> T newInstance(Name<T, M> name);

    class ClazzInstance implements Instantiation {

        public static final Instantiation INSTANCE = new ClazzInstance();

        @Override
        public <T, M> T newInstance(final Name<T, M> name) {
            try {
                return name == null ? null : name.getClazz().newInstance();
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
        }
    }

}
