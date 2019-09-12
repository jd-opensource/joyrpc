package io.joyrpc.permission;

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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常黑白名单
 */
public class ExceptionBlackWhiteList implements BlackWhiteList<Class<? extends Throwable>> {

    /**
     * 异常白名单，进行熔断
     */
    protected Set<Class<? extends Throwable>> whites;
    /**
     * 异常黑名单，不进行熔断
     */
    protected Set<Class<? extends Throwable>> blacks;
    /**
     * 检查的结果
     */
    protected Map<Class<? extends Throwable>, Detection> detects = new ConcurrentHashMap<>();
    /**
     * 空配置许可
     */
    protected boolean emptyPermission;

    /**
     * 构造函数
     *
     * @param whites
     * @param blacks
     */
    public ExceptionBlackWhiteList(final Set<Class<? extends Throwable>> whites,
                                   final Set<Class<? extends Throwable>> blacks) {
        this(whites, blacks, true);
    }

    /**
     * 构造函数
     *
     * @param whites
     * @param blacks
     * @param emptyPermission
     */
    public ExceptionBlackWhiteList(final Set<Class<? extends Throwable>> whites,
                                   final Set<Class<? extends Throwable>> blacks,
                                   final boolean emptyPermission) {
        this.whites = whites == null ? new HashSet<>() : whites;
        this.blacks = blacks == null ? new HashSet<>() : blacks;
        this.emptyPermission = emptyPermission;
    }

    @Override
    public boolean isValid(final Class<? extends Throwable> target) {
        if (target == null) {
            return false;
        } else if (whites.isEmpty() && blacks.isEmpty()) {
            //黑白名单为空
            return emptyPermission;
        } else {
            return detect(target).valid;
        }
    }

    @Override
    public boolean isBlack(final Class<? extends Throwable> target) {
        return target == null || blacks.isEmpty() ? false : detect(target).black;
    }

    @Override
    public void updateBlack(final Collection<Class<? extends Throwable>> targets) {
        Set<Class<? extends Throwable>> blacks = new HashSet<>(targets == null ? 0 : targets.size());
        if (targets != null) {
            blacks.addAll(targets);
        }
        this.blacks = blacks;
        this.detects = new ConcurrentHashMap<>();
    }

    @Override
    public boolean isWhite(final Class<? extends Throwable> target) {
        return target == null || whites.isEmpty() ? false : detect(target).white;
    }

    @Override
    public void updateWhite(final Collection<Class<? extends Throwable>> targets) {
        Set<Class<? extends Throwable>> whites = new HashSet<>(targets == null ? 0 : targets.size());
        if (targets != null) {
            whites.addAll(targets);
        }
        this.whites = whites;
        this.detects = new ConcurrentHashMap<>();
    }

    /**
     * 黑白名单不同时为空，检查该类及其父类，
     *
     * @param clazz
     * @return
     */
    protected Detection detect(final Class<? extends Throwable> clazz) {
        //判断是否已经检查过
        Detection result = detects.get(clazz);
        if (result != null) {
            return result;
        }
        Class parent = clazz;
        while (!Object.class.equals(parent)) {
            if (!blacks.isEmpty() && blacks.contains(parent)) {
                //黑名单里面
                result = new Detection(false, true, false);
                detects.put(clazz, result);
                return result;
            } else if (!whites.isEmpty() && whites.contains(parent)) {
                //白名单里面
                result = new Detection(true, false, true);
                detects.put(clazz, result);
                return result;
            }
            parent = parent.getSuperclass();
        }
        //不在黑白名单里面
        if (!whites.isEmpty()) {
            //如果白名单不为空，则其它的默认禁止
            result = new Detection(false);
            detects.put(clazz, result);
            return result;
        } else {
            //黑名单不为空，则其它的默认许可
            result = new Detection(true);
            detects.put(clazz, result);
            return result;
        }
    }

    /**
     * 检测接过
     */
    protected static class Detection {
        /**
         * 是否在白名单中
         */
        protected boolean white;
        /**
         * 是否在黑名单中
         */
        protected boolean black;
        /**
         * 是否有效
         */
        protected boolean valid;

        public Detection(boolean valid) {
            this.valid = valid;
        }

        public Detection(boolean white, boolean black, boolean valid) {
            this.white = white;
            this.black = black;
            this.valid = valid;
        }
    }
}
