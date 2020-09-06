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

import io.joyrpc.exception.MethodOverloadException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.*;
import java.util.List;

public class GenericTest {

    @Test
    public void testGeneric() throws NoSuchMethodException, MethodOverloadException {
        GenericClass genericClass = ClassUtils.getGenericClass(AppleService.class);
        Method method = ClassUtils.getPublicMethod(AppleService.class, "getPrice");
        GenericMethod genericMethod = genericClass.get(method);
        GenericType[] genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertEquals(genericTypes[0].getGenericType(), Apple.class);
        method = ClassUtils.getPublicMethod(AppleService.class, "add2ShopCar");
        genericMethod = genericClass.get(method);
        genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertTrue(genericTypes[0].getGenericType() instanceof GenericArrayType);
        Assert.assertEquals(((GenericArrayType) genericTypes[0].getGenericType()).getGenericComponentType(), Apple.class);
        method = ClassUtils.getPublicMethod(AppleService.class, "delete");
        genericMethod = genericClass.get(method);
        genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertTrue(genericTypes[0].getGenericType() instanceof ParameterizedType);
        Assert.assertEquals(((ParameterizedType) genericTypes[0].getGenericType()).getActualTypeArguments()[0], Apple.class);

        method = ClassUtils.getPublicMethod(AppleService.class, "update");
        genericMethod = genericClass.get(method);
        genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertTrue(genericTypes[0].getGenericType() instanceof TypeVariable);
        GenericType.Variable variable = genericTypes[0].getVariable(((TypeVariable) genericTypes[0].getGenericType()).getName());
        Assert.assertTrue(variable.getGenericType() instanceof TypeVariable);
        Type bound = ((TypeVariable) variable.getGenericType()).getBounds()[0];
        Assert.assertTrue(bound instanceof ParameterizedType);
        Assert.assertEquals(((ParameterizedType) bound).getActualTypeArguments()[0], Apple.class);

        method = ClassUtils.getPublicMethod(AppleService.class, "wildcard");
        genericMethod = genericClass.get(method);
        genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertTrue(genericTypes[0].getGenericType() instanceof ParameterizedType);
        Assert.assertTrue(((ParameterizedType) genericTypes[0].getGenericType()).getActualTypeArguments()[0] instanceof WildcardType);
        Assert.assertEquals(((WildcardType) ((ParameterizedType) genericTypes[0].getGenericType()).getActualTypeArguments()[0]).getUpperBounds()[0], Apple.class);
    }

    public static class Fruit {

    }

    public static class Apple extends Fruit {

        protected int weight;

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }

    public interface FruitService<T extends Fruit> {

        double getPrice(T fruit);

        void add2ShopCar(T[] fruits);

        void delete(List<T> fruits);

        <B extends List<T>> void update(B fruits);

        void wildcard(List<? extends T> fruits);

    }

    public interface AppleService extends FruitService<Apple> {

    }
}
