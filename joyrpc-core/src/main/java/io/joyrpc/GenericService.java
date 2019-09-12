package io.joyrpc;

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


import java.util.concurrent.CompletableFuture;

/**
 * 泛化调用接口，由于没有目标类，复杂参数对象采用Map进行传输（原理和JsonObject类似)
 * 泛型调用不会出现Callback
 */
public interface GenericService {

    /**
     * 泛化调用
     *
     * @param method         方法名
     * @param parameterTypes 参数类型
     * @param args           参数列表
     * @return 返回值
     */
    CompletableFuture<Object> $invoke(String method, String[] parameterTypes, Object[] args);

}
