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

import io.joyrpc.extension.service.Consumer;
import io.joyrpc.extension.spring.boot.SpringLoaderAutoConfiguration;
import io.joyrpc.extension.springboot.ConsumerAutoConfiguration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {SpringLoaderAutoConfiguration.class, ConsumerAutoConfiguration.class})
public class SpringMultiContextTest {

    @Autowired
    ApplicationContext parent;

    @Test
    public void testPlugin() {
        ConfigurableApplicationContext context = new AnnotationConfigApplicationContext(SpringLoaderAutoConfiguration.class, ConsumerAutoConfiguration.class);
        context.setParent(parent);
        context.start();
        ExtensionPoint<Consumer, String> consumer = new ExtensionPointLazy<Consumer, String>(Consumer.class);
        Assert.assertEquals(consumer.size(), 2);
        Consumer target = consumer.get();
        Assert.assertNotNull(target);
        context.close();
        Assert.assertEquals(consumer.size(), 1);
        target = consumer.get();
        Assert.assertNotNull(target);
    }
}
