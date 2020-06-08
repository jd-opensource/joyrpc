package io.joyrpc.util;

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

        method = ClassUtils.getPublicMethod(AppleService.class, "update");
        genericMethod = genericClass.get(method);
        genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertTrue(genericTypes[0].getType() instanceof TypeVariable);
        GenericType.Variable variable = genericTypes[0].getVariable(((TypeVariable) genericTypes[0].getType()).getName());
        Assert.assertTrue(variable.getType() instanceof TypeVariable);
        Type bound = ((TypeVariable) variable.getType()).getBounds()[0];
        Assert.assertTrue(bound instanceof ParameterizedType);
        Assert.assertEquals(((ParameterizedType) bound).getActualTypeArguments()[0], Apple.class);

        method = ClassUtils.getPublicMethod(AppleService.class, "wildcard");
        genericMethod = genericClass.get(method);
        genericTypes = genericMethod.getParameters();
        Assert.assertEquals(genericTypes.length, 1);
        Assert.assertTrue(genericTypes[0].getType() instanceof ParameterizedType);
        Assert.assertTrue(((ParameterizedType) genericTypes[0].getType()).getActualTypeArguments()[0] instanceof WildcardType);
        Assert.assertEquals(((WildcardType) ((ParameterizedType) genericTypes[0].getType()).getActualTypeArguments()[0]).getUpperBounds()[0], Apple.class);
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
