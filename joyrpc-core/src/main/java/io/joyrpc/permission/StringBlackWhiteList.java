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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 字符串黑白名单
 */
public class StringBlackWhiteList implements BlackWhiteList<String> {

    public static final Function<Set<String>, Boolean> MASK = (set) -> set != null && set.contains("*");

    /**
     * 白名单
     */
    protected volatile Set<String> whites;

    /**
     * 黑名单
     */
    protected volatile Set<String> blacks;
    /**
     * 允许所有函数
     */
    protected Function<Set<String>, Boolean> all = MASK;
    /**
     * 白名单允许所有
     */
    protected volatile boolean whiteAll;
    /**
     * 黑名单允许所有
     */
    protected volatile boolean blackAll;

    public StringBlackWhiteList() {
    }

    public StringBlackWhiteList(final Set<String> whites, final Set<String> blacks) {
        this(whites, blacks, MASK);
    }

    public StringBlackWhiteList(final Set<String> whites, final Set<String> blacks, final Function<Set<String>, Boolean> all) {
        this.whites = whites;
        this.blacks = blacks;
        this.all = all;
        this.blackAll = all != null && all.apply(blacks);
        this.whiteAll = all != null && all.apply(whites);
    }

    /**
     * 构造函数
     *
     * @param whites
     * @param blacks
     */
    public StringBlackWhiteList(final String whites, final String blacks) {
        this(whites == null || whites.isEmpty() ? null : new HashSet<>(Arrays.asList(split(whites, SEMICOLON_COMMA_WHITESPACE))),
                blacks == null || blacks.isEmpty() ? null : new HashSet<>(Arrays.asList(split(blacks, SEMICOLON_COMMA_WHITESPACE))),
                MASK);
    }

    /**
     * 构造函数，以逗号分隔黑白名单，黑名单以'-'开头
     *
     * @param blackWhiteList
     */
    public StringBlackWhiteList(final String blackWhiteList) {
        if (blackWhiteList != null && !blackWhiteList.isEmpty()) {
            String[] values = split(blackWhiteList, SEMICOLON_COMMA_WHITESPACE);
            Set<String> whites = new HashSet<>(values.length);
            Set<String> blacks = new HashSet<>(values.length);
            for (String value : values) {
                if (value.charAt(0) == '-') {
                    if (value.length() > 1) {
                        blacks.add(value.substring(1));
                    }
                } else {
                    whites.add(value);
                }
            }
            this.whites = whites;
            this.blacks = blacks;
            this.blackAll = all != null && all.apply(blacks);
            this.whiteAll = all != null && all.apply(whites);
        }
    }

    @Override
    public boolean isBlack(final String target) {
        return target != null && (blackAll || blacks != null && blacks.contains(target));
    }

    /**
     * 判断是否在白名单里面
     *
     * @param target
     * @return
     */
    public boolean inBlack(final String target) {
        return target != null && blacks != null && blacks.contains(target);
    }

    @Override
    public boolean isWhite(final String target) {
        return target != null && (whiteAll || whites != null && whites.contains(target));
    }

    @Override
    public boolean isValid(final String name) {
        return name != null
                && (blacks == null || blacks.isEmpty() || !blackAll && !blacks.contains(name))
                && (whites == null || whites.isEmpty() || whiteAll || whites.contains(name));
    }
}
