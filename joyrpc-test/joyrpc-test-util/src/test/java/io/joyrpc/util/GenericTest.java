package io.joyrpc.util;

import io.joyrpc.exception.MethodOverloadException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;

public class GenericTest {

    @Test
    public void testGeneric() throws NoSuchMethodException, MethodOverloadException {
        GenericClass genericClass = ClassUtils.getGenericClass(AppleService.class);
        Method method = ClassUtils.getPublicMethod(AppleService.class, "getPrice");
        GenericMethod genericMethod = genericClass.get(method);
        GenericType[] genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertEquals(genericTypes[0].getType(), Apple.class);
        method = ClassUtils.getPublicMethod(AppleService.class, "add2ShopCar");
        genericMethod = genericClass.get(method);
        genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertTrue(genericTypes[0].getType() instanceof GenericArrayType);
        Assert.assertEquals(((GenericArrayType) genericTypes[0].getType()).getGenericComponentType(), Apple.class);
        method = ClassUtils.getPublicMethod(AppleService.class, "delete");
        genericMethod = genericClass.get(method);
        genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertTrue(genericTypes[0].getType() instanceof ParameterizedType);
        Assert.assertEquals(((ParameterizedType) genericTypes[0].getType()).getActualTypeArguments()[0], Apple.class);
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

    }

    public interface AppleService extends FruitService<Apple> {

    }
}
