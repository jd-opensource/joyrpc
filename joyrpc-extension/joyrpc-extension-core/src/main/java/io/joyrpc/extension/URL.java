/**
 *
 */
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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiConsumer;

/**
 *
 */
public final class URL implements Serializable, Parametric {
    public static final String FILE = "file";
    public static final String UTF_8 = "UTF-8";
    private static final long serialVersionUID = -1985165475234910535L;
    private static final Map<String, String> UNMODIFIED_EMPTY_MAP = Collections.unmodifiableMap(new HashMap<String, String>());
    // 协议
    protected final String protocol;
    // 名称
    protected final String user;
    // 密码
    protected final String password;
    // 主机
    protected final String host;
    // 端口
    protected final int port;
    // 路径
    protected final String path;
    // 参数，只读不能修改
    protected final Map<String, String> parameters;

    protected URL() {
        this.protocol = null;
        this.user = null;
        this.password = null;
        this.host = null;
        this.port = 0;
        this.path = null;
        this.parameters = null;
    }

    public URL(String protocol, String host, int port) {
        this(protocol, null, null, host, port, null, null);
    }

    public URL(String protocol, String host, int port, Map<String, String> parameters) {
        this(protocol, null, null, host, port, null, parameters);
    }

    public URL(String protocol, String host, int port, String path) {
        this(protocol, null, null, host, port, path, null);
    }

    public URL(String protocol, String host, int port, String path, Map<String, String> parameters) {
        this(protocol, null, null, host, port, path, parameters);
    }

    public URL(String protocol, String user, String password, String host, int port, String path) {
        this(protocol, user, password, host, port, path, null);
    }

    public URL(String protocol, String user, String password, String host, int port, String path,
               Map<String, String> parameters) {
        this.protocol = protocol;
        this.user = user;
        this.password = password;
        this.host = host;
        this.port = (port < 0 ? 0 : port);
        this.path = path;
        this.parameters = parameters == null || parameters.isEmpty() ? UNMODIFIED_EMPTY_MAP :
                (UNMODIFIED_EMPTY_MAP.getClass().equals(parameters.getClass()) ? parameters :
                        Collections.unmodifiableMap(new HashMap<>(parameters)));
    }

    /**
     * 把字符串转化成URL对象
     *
     * @param url 字符串
     * @return 新创建的URL对象
     */
    public static URL valueOf(final String url) {
        return valueOf(url, null, null, null);
    }

    /**
     * 把字符串转化成URL对象
     *
     * @param source 字符串
     * @param defProtocol 默认的协议
     * @return 新创建的URL对象
     */
    public static URL valueOf(final String source, final String defProtocol) {
        return valueOf(source, defProtocol, null, null);
    }

    /**
     * 把字符串转化成URL对象
     *
     * @param source 字符串
     * @param defProtocol 默认的协议
     * @param params 参数
     * @return 新创建的URL对象
     */
    public static URL valueOf(final String source, final String defProtocol, final List<String> params) {
        return valueOf(source, defProtocol, null, params);
    }

    /**
     * 把字符串转化成URL对象
     *
     * @param source 字符串
     * @param defProtocol 默认的协议
     * @param defPort 默认端口
     * @param params 参数
     * @return 新创建的URL对象
     */
    public static URL valueOf(final String source, final String defProtocol, final Integer defPort, final List<String> params) {
        if (source == null) {
            return null;
        }
        String url = source.trim();
        if (url.isEmpty()) {
            return null;
        }
        String protocol = null;
        String user = null;
        String password = null;
        String host = null;
        int port = 0;
        String path = null;
        Map<String, String> parameters = null;

        // cloud://user:password@jss.360buy.com/mq?timeout=60000
        // file:/path/to/file.txt
        // zookeeper://10.10.10.10:2181,10.10.10.11:2181/?retryTimes=3
        // failover://(zookeeper://10.10.10.10:2181,10.10.10.11:2181;zookeeper://20.10.10.10:2181,20.10.10.11:2181)
        // ?interval=1000
        int j = 0;
        int i = url.indexOf(')');
        if (i >= 0) {
            i = url.indexOf('?', i);
        } else {
            i = url.indexOf("?");
        }
        if (i >= 0) {
            if (i < url.length() - 1) {
                String[] parts = url.substring(i + 1).split("&");
                parameters = new HashMap<String, String>(10);
                String name = null;
                String value = null;
                for (String part : parts) {
                    part = part.trim();
                    if (!part.isEmpty()) {
                        j = part.indexOf('=');
                        if (j > 0) {
                            name = part.substring(0, j);
                            value = j == part.length() - 1 ? "" : part.substring(j + 1);
                        } else if (j == -1) {
                            name = part;
                            value = part;
                        }
                        if (parameters.put(name, value) == null && params != null) {
                            params.add(name);
                        }
                    }
                }
            }
            url = url.substring(0, i);
        }
        i = url.indexOf("://");
        if (i > 0) {
            protocol = url.substring(0, i);
            url = url.substring(i + 3);
        } else if (i < 0) {
            // case: file:/path/to/file.txt
            // case: file:/D:\config
            i = url.indexOf(":/");
            if (i > 0) {
                protocol = url.substring(0, i);
                // 保留路径符号“/”
                url = url.substring(i + 2);
            }
        }
        if (protocol == null || protocol.isEmpty()) {
            protocol = defProtocol;
            if (protocol == null || protocol.isEmpty()) {
                throw new IllegalStateException("url missing protocol: " + url);
            }
        }
        if (protocol.equals(FILE)) {
            path = url;
            url = "";
        } else {
            i = url.lastIndexOf(')');
            if (i >= 0) {
                i = url.indexOf('/', i);
            } else {
                i = url.indexOf("/");
            }
            if (i >= 0) {
                path = url.substring(i + 1);
                url = url.substring(0, i);
            }
        }
        i = url.indexOf('(');
        if (i >= 0) {
            j = url.lastIndexOf(')');
            if (j >= 0) {
                url = url.substring(i + 1, j);
            } else {
                url = url.substring(i + 1);
            }
        } else {
            i = url.indexOf("@");
            if (i >= 0) {
                user = url.substring(0, i);
                j = user.indexOf(":");
                if (j >= 0) {
                    password = user.substring(j + 1);
                    user = user.substring(0, j);
                }
                url = url.substring(i + 1);
            }
            String[] values = url.split(":");
            if (values.length == 2) {
                // 排除zookeeper://192.168.1.2:2181,192.168.1.3:2181
                port = Integer.parseInt(values[1]);
                url = values[0];
            } else if (defPort != null) {
                port = defPort;
            }
        }
        if (!url.isEmpty()) {
            host = url;
        }
        return new URL(protocol, user, password, host, port, path, parameters);
    }

    /**
     * URL编码
     *
     * @param value 字符串
     * @return 编码后的字符串
     * @throws UnsupportedEncodingException
     */
    public static String encode(final String value) throws UnsupportedEncodingException {
        return encode(value, UTF_8);
    }

    /**
     * URL编码
     *
     * @param value   字符串
     * @param charset 字符集
     * @return 编码后的字符串
     * @throws UnsupportedEncodingException
     */
    public static String encode(final String value, final String charset) throws UnsupportedEncodingException {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return URLEncoder.encode(value, charset);
    }

    /**
     * URL解码
     *
     * @param value 编码后的字符串
     * @return 解码字符串
     * @throws UnsupportedEncodingException
     */
    public static String decode(String value) throws UnsupportedEncodingException {
        return decode(value, UTF_8);
    }

    /**
     * URL解码
     *
     * @param value 编码后的字符串
     * @return 解码字符串
     * @throws UnsupportedEncodingException
     */
    public static String decode(String value, String charset) throws UnsupportedEncodingException {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return URLDecoder.decode(value, charset == null || charset.isEmpty() ? UTF_8 : charset);
    }

    public String getProtocol() {
        return protocol;
    }

    public URL setProtocol(String protocol) {
        return new URL(protocol, user, password, host, port, path, parameters);
    }

    public String getUser() {
        return user;
    }

    public URL setUser(String user) {
        return new URL(protocol, user, password, host, port, path, parameters);
    }

    public String getPassword() {
        return password;
    }

    public URL setPassword(String password) {
        return new URL(protocol, user, password, host, port, path, parameters);
    }

    public String getHost() {
        return host;
    }

    public URL setHost(String host) {
        return new URL(protocol, user, password, host, port, path, parameters);
    }

    public int getPort() {
        return port;
    }

    public URL setPort(int port) {
        return new URL(protocol, user, password, host, port, path, parameters);
    }

    public String getAddress() {
        return port <= 0 ? host : host + ":" + port;
    }

    public URL setAddress(String address) {
        int i = address.lastIndexOf(':');
        String host;
        int port = this.port;
        if (i >= 0) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
        }
        return new URL(protocol, user, password, host, port, path, parameters);
    }

    public InetSocketAddress getSocketAddress() {
        return new InetSocketAddress(host, port);
    }

    public String getPath() {
        return path;
    }

    public URL setPath(String path) {
        return new URL(protocol, user, password, host, port, path, parameters);
    }

    public String getAbsolutePath() {
        if (path == null) {
            return null;
        }
        char first = path.charAt(0);
        if (first == '/') {
            return path;
        }
        // 判断是否是windows路径
        if (((first > 'a' && first < 'z') || (first > 'A' && first < 'Z')) && path.length() >= 3) {
            char second = path.charAt(1);
            char third = path.charAt(2);
            if (second == ':' && (third == '/' || third == '\\')) {
                return path;
            }
        }
        return "/" + path;
    }

    /**
     * 获取一份参数拷贝。
     * @return
     */
    public Map<String, String> getParameters() {
        return new HashMap<>(parameters);
    }

    /**
     * 获取指定后缀的非空参数
     * @param suffix
     * @return
     */
    public Map<String, String> endsWith(final String suffix) {
        Map<String, String> result = new HashMap<String, String>(10);
        if (suffix != null && !suffix.isEmpty()) {
            String key;
            String value;
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                if (key.endsWith(suffix) && value != null) {
                    result.put(key, value);
                }
            }
        }
        return result;
    }

    /**
     * 获取指定前缀的非空参数
     * @param prefix
     * @return
     */
    public Map<String, String> startsWith(final String prefix) {
        return startsWith(prefix, null);
    }

    /**
     * 获取指定前缀的非空参数
     * @param prefix 前缀
     * @param strip 是否裁剪前缀
     * @return
     */
    public Map<String, String> startsWith(final String prefix, final boolean strip) {
        return startsWith(prefix, strip ? Strip.SIMPLE_STRIP : null);
    }

    /**
     * 获取指定前缀的非空参数
     * @param prefix 前缀
     * @param strip Key裁剪函数
     * @return
     */
    public Map<String, String> startsWith(final String prefix, final Strip strip) {
        Map<String, String> result = new HashMap<String, String>(10);
        if (prefix != null && !prefix.isEmpty()) {
            String key;
            String value;
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                key = entry.getKey();
                value = entry.getValue();
                if (key.startsWith(prefix) && value != null) {
                    result.put(strip != null ? strip.apply(prefix, key) : key, value);
                }
            }
        }
        return result;
    }

    @Override
    public String getObject(final String key) {
        return getString(key);
    }

    @Override
    public String getString(final String key) {
        return parameters.get(key);
    }

    @Override
    public String getString(final String key, final String def) {
        String value = getString(key);
        if (value == null || value.isEmpty()) {
            return def;
        }
        return value;
    }

    @Override
    public String getString(final String key, final String candidate, final String def) {
        String value = getString(key);
        if (value == null || value.isEmpty()) {
            value = getString(candidate, def);
        }
        return value;
    }

    @Override
    public String getString(final URLOption<String> option) {
        return option == null ? null : getString(option.getName(), option.getValue());
    }

    @Override
    public String getString(final URLBiOption<String> option) {
        return option == null ? null : getString(option.getName(), option.getCandidate(), option.getValue());
    }

    /**
     * 获取URL解码后的参数值
     *
     * @param key 参数名称
     * @return 参数值
     * @throws UnsupportedEncodingException
     */
    public String getDecoded(final String key) throws UnsupportedEncodingException {
        return decode(getString(key));
    }

    /**
     * 获取URL解码后的参数值
     *
     * @param key     参数名称
     * @param charset 字符集
     * @return 参数值
     * @throws UnsupportedEncodingException
     */
    public String getDecoded(final String key, final String charset) throws UnsupportedEncodingException {
        return decode(getString(key), charset);
    }

    /**
     * 获取字符串参数值
     *
     * @param key     参数名称
     * @param def     默认值
     * @param charset 字符集
     * @return 参数值
     * @throws UnsupportedEncodingException
     */
    public String getDecoded(final String key, final String def, final String charset) throws UnsupportedEncodingException {
        return getDecoded(getString(key, def), charset);
    }

    @Override
    public Date getDate(final String key, final Date def) {
        return Converts.getDate(getString(key), def);
    }

    @Override
    public Date getDate(final String key, final SimpleDateFormat format) {
        return Converts.getDate(getString(key), format, null);
    }

    @Override
    public Date getDate(final String key, final SimpleDateFormat format, final Date def) {
        return Converts.getDate(getString(key), format, def);
    }

    @Override
    public Date getDate(final String key, final DateParser parser, final Date def) {
        return Converts.getDate(getObject(key), parser, def);
    }

    @Override
    public Float getFloat(final String key) {
        return Converts.getFloat(getString(key), null);
    }

    @Override
    public Float getFloat(final String key, final Float def) {
        return Converts.getFloat(getString(key), def);
    }

    @Override
    public Float getFloat(final String key, final String candidate, final Float def) {
        Float result = getFloat(key);
        return result != null ? result : getFloat(candidate, def);
    }

    @Override
    public Float getFloat(final URLOption<Float> option) {
        return option == null ? null : getFloat(option.getName(), option.getValue());
    }

    @Override
    public Float getFloat(final URLBiOption<Float> option) {
        return option == null ? null : getFloat(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Double getDouble(final String key) {
        return Converts.getDouble(getString(key), null);
    }

    @Override
    public Double getDouble(final String key, final String candidate, final Double def) {
        Double result = getDouble(key);
        return result != null ? result : getDouble(candidate, def);
    }

    @Override
    public Double getDouble(final String key, final Double def) {
        return Converts.getDouble(getString(key), def);
    }

    @Override
    public Double getDouble(final URLOption<Double> option) {
        return option == null ? null : getDouble(option.getName(), option.getValue());
    }

    @Override
    public Double getDouble(final URLBiOption<Double> option) {
        if (option == null) {
            return null;
        }
        Double result = getDouble(option.getName());
        if (result == null) {
            result = getDouble(option.getCandidate(), option.getValue());
        }
        return result;
    }

    @Override
    public Long getLong(final String key) {
        return Converts.getLong(getString(key), null);
    }

    @Override
    public Long getLong(final String key, final String candidate, final Long def) {
        Long result = getLong(key);
        return result != null ? result : getLong(candidate, def);
    }

    @Override
    public Long getLong(final String key, final Long def) {
        return Converts.getLong(getString(key), def);
    }

    @Override
    public Long getLong(final URLOption<Long> option) {
        return option == null ? null : getLong(option.getName(), option.getValue());
    }

    @Override
    public Long getLong(final URLBiOption<Long> option) {
        return option == null ? null : getLong(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Integer getInteger(final String key) {
        return Converts.getInteger(getString(key), null);
    }

    @Override
    public Integer getInteger(final String key, final Integer def) {
        return Converts.getInteger(getString(key), def);
    }

    @Override
    public Integer getInteger(final String key, final String candidate, final Integer def) {
        Integer result = getInteger(key);
        return result != null ? result : getInteger(candidate, def);
    }

    @Override
    public Integer getInteger(final URLOption<Integer> option) {
        return option == null ? null : getInteger(option.getName(), option.getValue());
    }

    @Override
    public Integer getInteger(final URLBiOption<Integer> option) {
        return option == null ? null : getInteger(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Short getShort(final String key) {
        return Converts.getShort(getString(key), null);
    }

    @Override
    public Short getShort(final String key, final Short def) {
        return Converts.getShort(getString(key), def);
    }

    @Override
    public Short getShort(final String key, final String candidate, final Short def) {
        Short result = getShort(key);
        return result != null ? result : getShort(candidate, def);
    }

    @Override
    public Short getShort(final URLOption<Short> option) {
        return option == null ? null : getShort(option.getName(), option.getValue());
    }

    @Override
    public Short getShort(final URLBiOption<Short> option) {
        return option == null ? null : getShort(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Byte getByte(final String key) {
        return Converts.getByte(getString(key), null);
    }

    @Override
    public Byte getByte(final String key, final Byte def) {
        return Converts.getByte(getString(key), def);
    }

    @Override
    public Byte getByte(final String key, final String candidate, final Byte def) {
        Byte result = getByte(key);
        return result != null ? result : getByte(candidate, def);
    }

    @Override
    public Byte getByte(final URLOption<Byte> option) {
        return option == null ? null : getByte(option.getName(), option.getValue());
    }

    @Override
    public Byte getByte(final URLBiOption<Byte> option) {
        return option == null ? null : getByte(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Boolean getBoolean(final String key) {
        return Converts.getBoolean(getString(key), null);
    }

    @Override
    public Boolean getBoolean(final String key, final Boolean def) {
        return Converts.getBoolean(getString(key), def);
    }

    @Override
    public Boolean getBoolean(final String key, final String candidate, final Boolean def) {
        Boolean result = getBoolean(key);
        return result != null ? result : getBoolean(candidate, def);
    }

    @Override
    public Boolean getBoolean(final URLOption<Boolean> option) {
        return option == null ? null : getBoolean(option.getName(), option.getValue());
    }

    @Override
    public Boolean getBoolean(final URLBiOption<Boolean> option) {
        return option == null ? null : getBoolean(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Long getNatural(final String key, final Long def) {
        return Converts.getNatural(getString(key), def);
    }

    @Override
    public Long getNatural(final String key, final String candidate, final Long def) {
        Long result = getNatural(key, (Long) null);
        return result != null ? result : getNatural(candidate, def);
    }

    @Override
    public Long getNaturalLong(final URLOption<Long> option) {
        return option == null ? null : getNatural(option.getName(), option.getValue());
    }

    @Override
    public Long getNaturalLong(final URLBiOption<Long> option) {
        return option == null ? null : getNatural(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Integer getNatural(final String key, final Integer def) {
        return Converts.getNatural(getString(key), def);
    }

    @Override
    public Integer getNatural(final String key, final String candidate, final Integer def) {
        Integer result = getNatural(key, (Integer) null);
        return result != null ? result : getNatural(candidate, def);
    }

    @Override
    public Integer getNaturalInt(final URLOption<Integer> option) {
        return option == null ? null : getNatural(option.getName(), option.getValue());
    }

    @Override
    public Integer getNaturalInt(final URLBiOption<Integer> option) {
        return option == null ? null : getNatural(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Short getNatural(final String key, final Short def) {
        return Converts.getNatural(getString(key), def);
    }

    @Override
    public Short getNatural(final String key, final String candidate, final Short def) {
        Short result = getNatural(key, (Short) null);
        return result != null ? result : getNatural(key, def);
    }

    @Override
    public Short getNaturalShort(final URLOption<Short> option) {
        return option == null ? null : getNatural(option.getName(), option.getValue());
    }

    @Override
    public Short getNaturalShort(final URLBiOption<Short> option) {
        return option == null ? null : getNatural(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Byte getNatural(final String key, final Byte def) {
        return Converts.getNatural(getString(key), def);
    }

    @Override
    public Byte getNatural(final String key, final String candidate, final Byte def) {
        Byte result = getNatural(key, (Byte) null);
        return result != null ? result : getNatural(candidate, def);
    }

    @Override
    public Byte getNaturalByte(final URLOption<Byte> option) {
        return option == null ? null : getNatural(option.getName(), option.getValue());
    }

    @Override
    public Byte getNaturalByte(final URLBiOption<Byte> option) {
        return option == null ? null : getNatural(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Long getPositive(final String key, final Long def) {
        return Converts.getPositive(getString(key), def);
    }

    @Override
    public Long getPositive(final String key, final String candidate, final Long def) {
        Long result = getPositive(key, (Long) null);
        return result != null ? result : getPositive(candidate, def);
    }

    @Override
    public Long getPositiveLong(final URLOption<Long> option) {
        return option == null ? null : getPositive(option.getName(), option.getValue());
    }

    @Override
    public Long getPositiveLong(final URLBiOption<Long> option) {
        return option == null ? null : getPositive(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Integer getPositive(final String key, final Integer def) {
        return Converts.getPositive(getString(key), def);
    }

    @Override
    public Integer getPositive(final String key, final String candidate, final Integer def) {
        Integer result = getPositive(key, (Integer) null);
        return result != null ? result : getPositive(candidate, def);
    }

    @Override
    public Integer getPositiveInt(final URLOption<Integer> option) {
        return option == null ? null : getPositive(option.getName(), option.getValue());
    }

    @Override
    public Integer getPositiveInt(final URLBiOption<Integer> option) {
        return option == null ? null : getPositive(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Short getPositive(final String key, final Short def) {
        return Converts.getPositive(getString(key), def);
    }

    @Override
    public Short getPositive(final String key, final String candidate, final Short def) {
        Short result = getPositive(key, (Short) null);
        return result != null ? result : getPositive(candidate, def);
    }

    @Override
    public Short getPositiveShort(final URLOption<Short> option) {
        return option == null ? null : getPositive(option.getName(), option.getValue());
    }

    @Override
    public Short getPositiveShort(final URLBiOption<Short> option) {
        return option == null ? null : getPositive(option.getName(), option.getCandidate(), option.getValue());
    }

    @Override
    public Byte getPositive(final String key, final Byte def) {
        return Converts.getPositive(getString(key), def);
    }

    @Override
    public Byte getPositive(final String key, final String candidate, final Byte def) {
        Byte result = getPositive(key, (Byte) null);
        return result != null ? result : getPositive(candidate, def);
    }

    @Override
    public Byte getPositiveByte(final URLOption<Byte> option) {
        return option == null ? null : getPositive(option.getName(), option.getValue());
    }

    @Override
    public Byte getPositiveByte(final URLBiOption<Byte> option) {
        return option == null ? null : getPositive(option.getName(), option.getCandidate(), option.getValue());
    }

    /**
     * 判断参数是否存在
     *
     * @param key 参数名称
     * @return <li>true 存在</li>
     * <li>false 不存在</li>
     */
    public boolean contains(final String key) {
        String value = getString(key);
        return value != null && !value.isEmpty();
    }

    /**
     * 添加布尔参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final boolean value) {
        return add(key, String.valueOf(value));
    }

    /**
     * 添加布尔参数
     *
     * @param option   选项
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<Boolean> option, final boolean value) {
        return option == null ? this : add(option.name, String.valueOf(value));
    }

    /**
     * 添加字符参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final char value) {
        return add(key, String.valueOf(value));
    }

    /**
     * 添加字符参数
     *
     * @param option   选项
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<Character> option, final char value) {
        return option == null ? this : add(option.name, String.valueOf(value));
    }

    /**
     * 添加字节参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final byte value) {
        return add(key, String.valueOf(value));
    }

    /**
     * 添加字节参数
     *
     * @param option   选项
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<Byte> option, final byte value) {
        return option == null ? this : add(option.name, String.valueOf(value));
    }

    /**
     * 添加短整数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final short value) {
        return add(key, String.valueOf(value));
    }

    /**
     * 添加短整数参数
     *
     * @param option   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<Short> option, final short value) {
        return option == null ? this : add(option.name, String.valueOf(value));
    }

    /**
     * 添加整数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final int value) {
        return add(key, String.valueOf(value));
    }

    /**
     * 添加整数参数
     *
     * @param option   选项
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<Integer> option, final int value) {
        return option == null ? this : add(option.name, String.valueOf(value));
    }

    /**
     * 添加长整数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final long value) {
        return add(key, String.valueOf(value));
    }

    /**
     * 添加长整数参数
     *
     * @param option   选项
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<Long> option, final long value) {
        return option == null ? this : add(option.name, String.valueOf(value));
    }

    /**
     * 添加单精度浮点数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final float value) {
        return add(key, String.valueOf(value));
    }

    /**
     * 添加单精度浮点数参数
     *
     * @param option   选项
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<Float> option, final float value) {
        return option == null ? this : add(option.name, String.valueOf(value));
    }

    /**
     * 添加双精度浮点数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final double value) {
        return add(key, String.valueOf(value));
    }

    /**
     * 添加双精度浮点数参数
     *
     * @param option   选项
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<Double> option, final double value) {
        return option == null ? this : add(option.name, String.valueOf(value));
    }

    /**
     * 添加数字参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final Number value) {
        return add(key, value == null ? (String) null : String.valueOf(value));
    }

    /**
     * 添加字符序列参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final CharSequence value) {
        return add(key, value == null ? (String) null : value.toString());
    }

    /**
     * 添加字符串参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final String key, final String value) {
        if (key == null || key.isEmpty()) {
            return this;
        }
        Map<String, String> map = getParameters();
        map.put(key, value);
        return new URL(protocol, user, password, host, port, path, map);
    }

    /**
     * 添加字符串参数
     *
     * @param option 选项
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL add(final URLOption<String> option, final String value) {
        return option == null ? this : add(option.name, value);
    }

    /**
     * 添加参数
     *
     * @param url url
     * @return 新创建的URL对象
     */
    public URL add(final URL url) {
        if (url == null || url.parameters.isEmpty()) {
            return this;
        }
        Map<String, String> map = getParameters();
        map.putAll(url.parameters);
        return new URL(protocol, user, password, host, port, path, map);
    }

    /**
     * 添加参数
     *
     * @param parameters 参数
     * @return 新创建的URL对象
     */
    public URL add(final Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return this;
        }
        Map<String, String> map = getParameters();
        map.putAll(parameters);
        return new URL(protocol, user, password, host, port, path, map);
    }

    /**
     * 添加字符串参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     * @throws UnsupportedEncodingException
     */
    public URL addEncoded(final String key, final String value) throws UnsupportedEncodingException {
        return add(key, encode(value));
    }

    /**
     * 添加字符串参数
     *
     * @param key     参数名称
     * @param value   值
     * @param charset 字符集
     * @return 新创建的URL对象
     * @throws UnsupportedEncodingException
     */
    public URL addEncoded(final String key, final String value, final String charset) throws UnsupportedEncodingException {
        return add(key, encode(value, charset));
    }

    /**
     * 添加字符串参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     * @throws UnsupportedEncodingException
     */
    public URL addEncoded(final String key, final CharSequence value) throws UnsupportedEncodingException {
        return add(key, encode(value == null ? null : value.toString()));
    }

    /**
     * 添加字符串参数
     *
     * @param key     参数名称
     * @param value   值
     * @param charset 字符集
     * @return 新创建的URL对象
     * @throws UnsupportedEncodingException
     */
    public URL addEncoded(final String key, final CharSequence value, final String charset) throws UnsupportedEncodingException {
        return add(key, encode(value == null ? null : value.toString(), charset));
    }

    /**
     * 添加布尔参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final boolean value) {
        return addIfAbsent(key, String.valueOf(value));
    }

    /**
     * 添加字符参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final char value) {
        return addIfAbsent(key, String.valueOf(value));
    }

    /**
     * 添加字节参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final byte value) {
        return addIfAbsent(key, String.valueOf(value));
    }

    /**
     * 添加短整数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final short value) {
        return addIfAbsent(key, String.valueOf(value));
    }

    /**
     * 添加整数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final int value) {
        return addIfAbsent(key, String.valueOf(value));
    }

    /**
     * 添加长整数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final long value) {
        return addIfAbsent(key, String.valueOf(value));
    }

    /**
     * 添加单精度浮点数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final float value) {
        return addIfAbsent(key, String.valueOf(value));
    }

    /**
     * 添加双精度浮点数参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final double value) {
        return addIfAbsent(key, String.valueOf(value));
    }

    /**
     * 添加数字参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final Number value) {
        return addIfAbsent(key, value == null ? (String) null : String.valueOf(value));
    }

    /**
     * 添加字符序列参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final CharSequence value) {
        return addIfAbsent(key, value == null ? (String) null : value.toString());
    }

    /**
     * 如果参数不存在，则添加字符串参数
     *
     * @param key   参数名称
     * @param value 值
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final String key, final String value) {
        if (key == null || key.isEmpty() || value == null || value.isEmpty() || contains(key)) {
            return this;
        }
        Map<String, String> map = getParameters();
        map.put(key, value);
        return new URL(protocol, user, password, host, port, path, map);
    }

    /**
     * 添加不存在的参数
     *
     * @param url url
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final URL url) {
        if (url == null || url.parameters.isEmpty()) {
            return this;
        }
        Map<String, String> map = url.getParameters();
        map.putAll(this.parameters);
        return new URL(protocol, user, password, host, port, path, map);
    }

    /**
     * 添加不存在的参数
     *
     * @param parameters 参数
     * @return 新创建的URL对象
     */
    public URL addIfAbsent(final Map<String, String> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return this;
        }
        //复制一份数据
        Map<String, String> map = new HashMap<String, String>(parameters);
        map.putAll(this.parameters);
        return new URL(protocol, user, password, host, port, path, map);
    }

    /**
     * 删除参数
     *
     * @param key 参数
     * @return 新的URL对象
     */
    public URL remove(final String key) {
        if (key == null || key.isEmpty()) {
            return this;
        }
        return remove(new String[]{key});
    }

    /**
     * 删除参数
     *
     * @param keys 参数
     * @return 新的URL对象
     */
    public URL remove(final Collection<String> keys) {
        if (keys == null || keys.size() == 0) {
            return this;
        }
        return remove(keys.toArray(new String[keys.size()]));
    }

    /**
     * 删除参数
     *
     * @param keys 参数
     * @return 新的URL对象
     */
    public URL remove(final String... keys) {
        if (keys == null || keys.length == 0) {
            return this;
        }
        Map<String, String> map = getParameters();
        for (String key : keys) {
            map.remove(key);
        }
        if (parameters.size() == map.size()) {
            return this;
        }
        return new URL(protocol, user, password, host, port, path, map);
    }

    /**
     * 删除所有参数
     *
     * @return 新的URL对象
     */
    public URL remove() {
        return new URL(protocol, user, password, host, port, path, new HashMap<String, String>());
    }

    @Override
    public void foreach(final BiConsumer<String, Object> consumer) {
        if (consumer != null && parameters != null) {
            parameters.forEach(consumer::accept);
        }
    }

    /**
     * 转换成字符串，不包括用户信息
     *
     * @return 字符串表示
     */
    @Override
    public String toString() {
        return toString(false, true); // no show user and password
    }

    /**
     * 构建字符串
     *
     * @param user       是否要带用户
     * @param parameter  是否要带参数
     * @param parameters 指定参数
     * @return 字符串
     */
    public String toString(final boolean user, final boolean parameter, final String... parameters) {
        StringBuilder buf = new StringBuilder();
        if (protocol != null && !protocol.isEmpty()) {
            buf.append(protocol).append("://");
        }
        if (user && this.user != null && !this.user.isEmpty()) {
            buf.append(this.user);
            if (password != null && !password.isEmpty()) {
                buf.append(':').append(password);
            }
            buf.append('@');
        }
        boolean address = false;
        if (host != null && !host.isEmpty()) {
            address = true;
            buf.append(host);
            if (port > 0) {
                buf.append(':').append(port);
            }
        }
        if (path != null && !path.isEmpty()) {
            if (address) {
                buf.append('/');
            }
            buf.append(path);
        }
        if (parameter) {
            append(buf, true, parameters);
        }
        return buf.toString();
    }

    /**
     * 追加参数
     *
     * @param buf        缓冲器
     * @param concat     是否追加参数连接符号"?"
     * @param parameters 参数名称
     */
    protected void append(final StringBuilder buf, final boolean concat, final String[] parameters) {
        Map<String, String> map = this.parameters;
        if (map != null && !map.isEmpty()) {
            boolean first = true;
            String value;
            if (parameters != null && parameters.length > 0) {
                Set<String> includes = new TreeSet<String>();
                for (String p : parameters) {
                    if (p != null && !p.isEmpty()) {
                        includes.add(p);
                    }
                }
                for (String p : includes) {
                    value = map.get(p);
                    if (first) {
                        if (concat) {
                            buf.append('?');
                        }
                        first = false;
                    } else {
                        buf.append('&');
                    }
                    buf.append(p).append('=');
                    if (value != null) {
                        buf.append(value.trim());
                    }
                }
            } else {
                String key;
                // 按照字符串排序
                for (Map.Entry<String, String> entry : new TreeMap<String, String>(map).entrySet()) {
                    key = entry.getKey();
                    value = entry.getValue();
                    if (key != null && !key.isEmpty()) {
                        if (first) {
                            if (concat) {
                                buf.append('?');
                            }
                            first = false;
                        } else {
                            buf.append('&');
                        }
                        buf.append(key).append('=');
                        if (value != null) {
                            buf.append(value.trim());
                        }
                    }
                }
            }
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + port;
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        URL other = (URL) obj;
        if (protocol == null) {
            if (other.protocol != null) {
                return false;
            }
        } else if (!protocol.equals(other.protocol)) {
            return false;
        }
        if (host == null) {
            if (other.host != null) {
                return false;
            }
        } else if (!host.equals(other.host)) {
            return false;
        }
        if (port != other.port) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        return true;
    }
}
