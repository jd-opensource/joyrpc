package io.joyrpc.codec.serialization.model;

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

import java.util.Arrays;
import java.util.Objects;

public class ArrayObject {

    public String[] strArray;
    public Foo[] fooArray;
    public Object[] objArray;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrayObject that = (ArrayObject) o;

        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(strArray, that.strArray)) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(fooArray, that.fooArray)) {
            return false;
        }
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        return Arrays.equals(objArray, that.objArray);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(strArray);
        result = 31 * result + Arrays.hashCode(fooArray);
        result = 31 * result + Arrays.hashCode(objArray);
        return result;
    }

    public static final class Foo {
        protected String name;
        protected int id;

        public Foo() {
        }

        public Foo(String name, int id) {
            this.name = name;
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Foo foo = (Foo) o;

            if (id != foo.id) {
                return false;
            }
            return name != null ? name.equals(foo.name) : foo.name == null;
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + id;
            return result;
        }
    }
}
