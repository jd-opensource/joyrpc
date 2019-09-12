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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UrlTest {

    @Test
    public void test() {
        URL url = URL.valueOf("http://${user.name}:${user.password}@xxxxx/sadfsfasf");
        System.out.println(url.toString(true, true));
        List<String> params = new ArrayList<String>(3);
        url = URL.valueOf("http://${xxxx:user.name}:${user.password}@xxxxx/sadfsfasf?name=3&key=x", "http", params);
        System.out.println(url.getUser());
        Assert.assertTrue(params.size() == 2);
        url = URL.valueOf("file:/D:\\config");
        Assert.assertEquals(url.getPath(), "D:\\config");
        Assert.assertEquals(url.getAbsolutePath(), "D:\\config");
        url = URL.valueOf("http://yyy?xxx=b");
        String a = url.toString(false, true);
        String a1 = url.toString(false, true, "xxx");
        Assert.assertEquals(a, a1);

    }
}
