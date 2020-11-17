package io.joyrpc.extension.test;

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

import io.joyrpc.extension.ExtensionPoint;
import io.joyrpc.extension.ExtensionPointLazy;
import io.joyrpc.extension.api.Consumer;
import io.joyrpc.extension.api.Producer;
import io.joyrpc.extension.boot.ExtensionAutoConfiguration;
import io.joyrpc.extension.spring.boot.SpringLoaderAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SpringLoaderAutoConfiguration.class, ExtensionAutoConfiguration.class})
public class SpringTest {

    @Test
    public void testPlugin() {
        ExtensionPoint<Consumer, String> consumer = new ExtensionPointLazy<>(Consumer.class);
        Assertions.assertNotNull(consumer.get("myConsumer3"));
        ExtensionPoint<Producer, String> producer = new ExtensionPointLazy<>(Producer.class);
        Assertions.assertNotNull(producer.get("myProducer1"));
    }
}
