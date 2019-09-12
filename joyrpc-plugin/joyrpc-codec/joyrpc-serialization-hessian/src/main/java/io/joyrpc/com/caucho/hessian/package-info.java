package io.joyrpc.com.caucho.hessian;

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

/**
 * 修改记录：
 * 1. 修改io.joyrpc.com.caucho.hessian.io.MapSerializer类，添加对LinkedHashMap的特殊处理
 * 2. 修改io.joyrpc.com.caucho.hessian.io.CollectionDeserializer类，添加对LinkedHashMap的特殊处理
 * 3. 修改io.joyrpc.com.caucho.hessian.io.ContextSerializerFactory类，添加locale支持
 * 4. 添加io.joyrpc.com.caucho.hessian.io.java8包下类文件，添加jdk8日期支持，添加zoneregion支持
 */
