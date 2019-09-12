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

/**
 * 扩展点实例
 */
public class Plugin<T> {
    //实例元数据
    protected Name<? extends T, String> name;
    //实例化接口
    protected Instantiation instantiation;
    //是否是单例
    protected Boolean singleton;
    //单例
    protected T target;
    //加载器
    protected Object loader;

    public Plugin() {
    }

    public Plugin(Name<? extends T, String> name, T target, Object loader) {
        this.name = name;
        this.target = target;
        this.loader = loader;
    }

    public Plugin(Name<? extends T, String> name, Instantiation instantiation, Boolean singleton, T target, Object loader) {
        this.name = name;
        this.instantiation = instantiation;
        this.singleton = singleton;
        this.target = target;
        this.loader = loader;
    }

    public Name<? extends T, String> getName() {
        return name;
    }

    public void setName(Name<? extends T, String> name) {
        this.name = name;
    }

    public Boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(Boolean singleton) {
        this.singleton = singleton;
    }

    public T getTarget() {
        return target;
    }

    public void setTarget(T target) {
        this.target = target;
    }

    public Instantiation getInstantiation() {
        return instantiation;
    }

    public void setInstantiation(Instantiation instantiation) {
        this.instantiation = instantiation;
    }

    public Object getLoader() {
        return loader;
    }

    public void setLoader(Object loader) {
        this.loader = loader;
    }
}
