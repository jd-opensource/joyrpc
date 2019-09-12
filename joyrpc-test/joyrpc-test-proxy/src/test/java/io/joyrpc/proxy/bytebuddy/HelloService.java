package io.joyrpc.proxy.bytebuddy;

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

import java.util.List;
import java.util.Map;

/**
 * @date: 1 /23/2019
 */
public interface HelloService {

    /**
     * Say hello string.
     *
     * @return the string
     */
    public String sayHello();

    /**
     * Throw exception string.
     *
     * @param map the map
     * @return the string
     * @throws Exception the exception
     */
    public String throwException(Map map) throws Exception;

    /**
     * Save hello.
     *
     * @param hello the hello
     */
    public void saveHello(Hello hello);

    /**
     * Gets hello.
     *
     * @param id the id
     * @return the hello
     */
    public Hello getHello(Long id);

    /**
     * Search hello list.
     *
     * @param s the s
     * @param i the
     * @param l the l
     * @return the list
     */
    public List<Hello> searchHello(String s, Integer i, Integer l);

    /**
     * Build dict map.
     *
     * @param list the list
     * @return the map
     */
    public Map buildDict(List list);
}
