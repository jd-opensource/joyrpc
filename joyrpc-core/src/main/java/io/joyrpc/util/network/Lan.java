package io.joyrpc.util.network;

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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static io.joyrpc.util.StringUtils.SEMICOLON_COMMA_WHITESPACE;
import static io.joyrpc.util.StringUtils.split;

/**
 * 局域网,多个网段由逗号或分号隔开
 * 单个网段格式如下：
 * <li>172.168.1.0/24</li>
 * <li>172.168.1.0-172.168.1.255</li>
 * <li>172.168.1.1</li>
 * <li>172.168.1.*</li>
 */
public class Lan {
    // 多个网段
    private List<Segment> segments = new ArrayList<Segment>();
    // ID
    private int id;
    // 名称
    private String name;

    public Lan(String ips) {
        this(0, null, ips, false);
    }

    public Lan(String ips, boolean ignoreError) {
        this(0, null, ips, ignoreError);
    }

    public Lan(int id, String name, String ips) {
        this(id, name, ips, false);
    }

    public Lan(int id, String name, String ips, boolean ignoreError) {
        this.id = id;
        this.name = name;
        if (ips != null && !ips.isEmpty()) {
            String[] parts = split(ips, SEMICOLON_COMMA_WHITESPACE);
            for (String part : parts) {
                try {
                    segments.add(new Segment(part));
                } catch (IllegalArgumentException e) {
                    if (!ignoreError) {
                        throw e;
                    }
                }
            }
        }
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * 是否包含指定IP
     *
     * @param ip IP
     * @return 布尔值
     */
    public boolean contains(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        IpLong ipLong = new IpLong(ip);
        return contains(segment -> segment.contains(ipLong));
    }

    /**
     * 是否包含指定IP
     *
     * @param predicate 断言
     * @return 布尔值
     */
    protected boolean contains(final Predicate<Segment> predicate) {
        if (segments.isEmpty()) {
            //没有指定，则是全网
            return true;
        }
        for (Segment segment : segments) {
            if (predicate.test(segment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否包含指定IP
     *
     * @param ip IP
     * @return 布尔值
     */
    public boolean contains(IpLong ip) {
        return contains(segment -> segment.contains(ip));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Lan lan = (Lan) o;

        if (id != lan.id) {
            return false;
        }
        if (!segments.equals(lan.segments)) {
            return false;
        }
        return name != null ? name.equals(lan.name) : lan.name == null;

    }

    @Override
    public int hashCode() {
        int result = segments.hashCode();
        result = 31 * result + id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(100);
        int count = 0;
        for (Segment segment : segments) {
            if (count++ > 0) {
                builder.append(';');
            }
            builder.append(segment.toString());
        }
        return builder.toString();
    }
}
