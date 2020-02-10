package io.joyrpc.util;

import io.joyrpc.util.model.User;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Map;

public class ClassUtilsTest {

    @Test
    public void testGetterSetter() {
        Map<String, Method> getter = ClassUtils.getGetter(User.class);
        Assert.assertEquals(getter.size(), 2);
        Assert.assertTrue(getter.containsKey("name"));
        Assert.assertTrue(getter.containsKey("man"));
    }
}
