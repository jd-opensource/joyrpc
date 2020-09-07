/**
 *
 */
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

import java.io.IOException;
import java.net.*;
import java.util.*;

/**
 * Ipv4工具
 *
 * IPv6 地址大小为 128 位。首选 IPv6 地址表示法为 x:x:x:x:x:x:x:x，其中每个 x 是地址的 8 个 16 位部分的十六进制值。IPv6 地址范围从 0000:0000:0000:0000:0000:0000:0000:0000 至 ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff。<br/>
 * 除此首选格式之外，IPv6 地址还可以用其他两种短格式指定：<br/>
 * <li>省略前导零</li><br/>
 * 通过省略前导零指定 IPv6 地址。例如，IPv6 地址 1050:0000:0000:0000:0005:0600:300c:326b 可写作 1050:0:0:0:5:600:300c:326b。
 * <li>双冒号</li><br/>
 * 通过使用双冒号（::）替换一系列零来指定 IPv6 地址。例如，IPv6 地址 ff06:0:0:0:0:0:0:c3 可写作 ff06::c3。一个 IP 地址中只可使用一次双冒号。<br/>
 * IPv6 地址的替代格式组合了冒号与点分表示法，因此可将 IPv4 地址嵌入到 IPv6 地址中。对最左边 96 个位指定十六进制值，对最右边 32 个位指定十进制值，来指示嵌入的 IPv4 地址。在混合的网络环境中工作时，此格式确保 IPv6 节点和 IPv4 节点之间的兼容性。<br/>
 * IPv4 映射的 IPv6 地址使用此替代格式。此类型的地址用于将 IPv4 节点表示为 IPv6 地址。它允许 IPv6 应用程序直接与 IPv4 应用程序通信。例如，0:0:0:0:0:ffff:192.1.56.10 和 ::ffff:192.1.56.10/96（短格式）<br/>
 *
 */
public class Ipv4 {

    /**
     * 管理IP
     */
    public static String MANAGE_IP;
    /**
     * 网卡
     */
    public static String NET_INTERFACE;
    /**
     * 任意地址
     */
    public static final String ANYHOST = "0.0.0.0";
    /**
     * 内网地址
     */
    public static final Lan INTRANET = new Lan("172.16.0.0/12;192.168.0.0/16;10.0.0.0/8");
    /**
     * 本地绑定的网卡
     */
    public static final String LOCAL_NIC_KEY = "LOCAL_NIC";
    /**
     * 管理IP
     */
    public static final String MANAGE_IP_KEY = "MANAGE_IP";
    /**
     * 最小端口
     */
    public static final int MIN_PORT = 0;
    /**
     * 最小用户端口
     */
    public static final int MIN_USER_PORT = 1025;
    /**
     * 最大端口
     */
    public static final int MAX_PORT = 65535;

    /**
     * 最大用户端口
     */
    public static final int MAX_USER_PORT = 65534;

    /**
     * 本地所有IP
     */
    protected static Set<String> LOCAL_IPS;

    /**
     * 本地名称
     */
    protected static Set<String> LOCAL_HOST = new HashSet<>(5);
    /**
     * 本地首选的IP
     */
    protected static String LOCAL_IP;

    protected static boolean IPV4 = false;

    public static final IpLong IP_MIN;
    public static final IpLong IP_MAX;

    static {
        //从环境变量里面获取默认的网卡和管理网络
        Map<String, String> env = System.getenv();
        NET_INTERFACE = System.getProperty(LOCAL_NIC_KEY, env.get(LOCAL_NIC_KEY));
        MANAGE_IP = System.getProperty(MANAGE_IP_KEY, env.get(MANAGE_IP_KEY));
        try {
            //java.net.preferIPv6Addresses表示在查询本地或远端IP地址时，如果存在IPv4和IPv6双地址，是否优先返回IPv6地址，默认是false
            IPV4 = InetAddress.getLocalHost() instanceof Inet4Address;
        } catch (UnknownHostException ignored) {
        }
        IP_MIN = IPV4 ? new IpLong("0.0.0.0") : new IpLong("0000:0000:0000:0000:0000:0000:0000:0000");
        IP_MAX = IPV4 ? new IpLong("255.255.255.255") : new IpLong("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF");
        try {
            LOCAL_IPS = new HashSet<>(getLocalIps());
            LOCAL_IPS.add("127.0.0.1");
            LOCAL_IPS.add("0:0:0:0:0:0:0:1");
            LOCAL_IPS.add("0000:0000:0000:0000:0000:0000:0000:0001");
            LOCAL_IPS.add("::1");
            LOCAL_HOST.add("localhost");
            LOCAL_HOST.add("127.0.0.1");
            LOCAL_HOST.add("0:0:0:0:0:0:0:1");
            LOCAL_HOST.add("0000:0000:0000:0000:0000:0000:0000:0001");
            LOCAL_HOST.add("::1");
            //绑定到某个网卡上
            if (NET_INTERFACE != null && !NET_INTERFACE.isEmpty()) {
                LOCAL_IP = getLocalIp(NET_INTERFACE, MANAGE_IP);
            }
        } catch (SocketException ignored) {
        }
    }

    /**
     * 是否启用IPV4
     * @return 启用IPV4标识
     */
    public static boolean isIpv4() {
        return IPV4;
    }

    /**
     * 是否默认地址 0.0.0.0
     *
     * @param host 地址
     * @return 是否默认地址
     */
    public static boolean isAnyHost(String host) {
        return ANYHOST.equals(host);
    }

    /**
     * 是否是本地IP
     * @param ip ip
     * @return 本地IP标识
     */
    public static boolean isLocalIp(final String ip) {
        return ip != null && LOCAL_IPS != null && LOCAL_IPS.contains(ip);
    }

    /**
     * 得到本机所有的地址
     *
     * @return 本机所有的地址
     * @throws SocketException 网络异常
     */
    public static List<String> getLocalIps() throws SocketException {
        return getLocalIps(null, null);
    }

    /**
     * 得到指定网卡上的地址
     *
     * @param nic     网卡
     * @param exclude 排除的地址
     * @return 地址列表
     * @throws SocketException 网络异常
     */
    public static List<String> getLocalIps(final String nic, final String exclude) throws SocketException {
        List<String> result = new ArrayList<String>();
        NetworkInterface ni;
        Enumeration<InetAddress> ias;
        InetAddress address;
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        while (netInterfaces.hasMoreElements()) {
            ni = netInterfaces.nextElement();
            if (nic != null && !nic.isEmpty() && !ni.getName().equals(nic)) {
                continue;
            }
            ias = ni.getInetAddresses();
            while (ias.hasMoreElements()) {
                address = ias.nextElement();
                if (!address.isLoopbackAddress()) {
                    if (IPV4 && address instanceof Inet4Address
                            || !IPV4 && address instanceof Inet6Address) {
                        result.add(toIp(address));
                    }
                }
            }
        }
        // 只有一个IP
        int count = result.size();
        if (count <= 1) {
            return result;
        }
        if (exclude != null && !exclude.isEmpty()) {
            String ip;
            // 多个IP，排除IP
            for (int i = count - 1; i >= 0; i--) {
                ip = result.get(i);
                if (ip.startsWith(exclude)) {
                    // 删除排除的IP
                    result.remove(i);
                    count--;
                    if (count == 1) {
                        // 确保有一个IP
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * 得到本机内网地址
     *
     * @param nic      网卡
     * @param manageIp 管理段IP地址
     * @return 本机地址
     * @throws SocketException
     */
    public static String getLocalIp(final String nic, final String manageIp) throws SocketException {
        List<String> ips = getLocalIps(nic, manageIp);
        if (!ips.isEmpty()) {
            if (ips.size() == 1) {
                return ips.get(0);
            }
            for (String ip : ips) {
                if (INTRANET.contains(ip)) {
                    return ip;
                }
            }
            return ips.get(0);
        }
        return null;
    }

    /**
     * 得到本机内网地址
     *
     * @param manageIp 管理段IP地址
     * @return 本机地址
     * @throws SocketException
     */
    public static String getLocalIp(final String manageIp) throws SocketException {
        return getLocalIp(NET_INTERFACE, manageIp);
    }

    /**
     * 得到本机内网地址
     *
     * @return 本机地址
     */
    public static String getLocalIp() {
        if (LOCAL_IP == null) {
            try {
                LOCAL_IP = getLocalIp(NET_INTERFACE, MANAGE_IP);
            } catch (SocketException ignored) {
            }
        }
        return LOCAL_IP;
    }

    /**
     * 得到本机内网地址，通过远端地址进行探测
     *
     * @param remote 远端地址
     * @return 本机地址
     */
    public static String getLocalIp(final InetSocketAddress remote) {
        if (remote == null) {
            return getLocalIp();
        }
        if (LOCAL_IP == null) {
            try {
                InetAddress address = getLocalAddress(remote);
                LOCAL_IP = address.getHostAddress();
            } catch (IOException e) {
                getLocalIp();
            }
        }
        return LOCAL_IP;
    }

    /**
     * 是否是本地域名
     * @param host 主机
     * @return 本地域名标识
     */
    public static boolean isLocalHost(final String host) {
        return host == null || host.isEmpty() || LOCAL_HOST.contains(host);
    }

    /**
     * 判断端口是否有效 0-65535
     *
     * @param port
     *         端口
     * @return 是否有效
     */
    public static boolean isValidPort(final int port) {
        return port <= MAX_PORT && port >= MIN_PORT;
    }

    /**
     * 判断端口是否为有效用户端口 1025-65534
     *
     * @param port
     *         端口
     * @return 是否有效
     */
    public static boolean isValidUserPort(final int port) {
        return port <= MAX_USER_PORT && port >= MIN_USER_PORT;
    }

    /**
     * 通过连接远程地址得到本机内网地址
     *
     * @param remote 远程地址
     * @return 本机内网地址
     */
    public static InetAddress getLocalAddress(final InetSocketAddress remote) throws IOException {
        if (remote == null) {
            return null;
        }
        // 去连一下远程地址
        try (Socket socket = new Socket()) {
            socket.connect(remote, 1000);
            // 得到本地地址
            return socket.getLocalAddress();
        }
    }

    /**
     * 判断该IP是否在网段中
     * @param segments 网段
     * @param ip ip
     * @return IP是否在网段中
     */
    public static boolean contains(final List<Segment> segments, final String ip) {
        if (segments == null || segments.isEmpty() || ip == null || ip.isEmpty()) {
            return false;
        }
        try {
            IpLong v = new IpLong(ip);
            for (Segment segment : segments) {
                if (segment.contains(v)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * 判断该IP是否在网络短中
     * @param segments 网段
     * @param ip ip
     * @return 布尔值
     */
    public static boolean contains(final List<Segment> segments, final long ip) {
        if (segments == null || segments.isEmpty()) {
            return false;
        }
        IpLong ipLong = new IpLong(ip);
        for (Segment segment : segments) {
            if (segment.contains(ipLong)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把地址对象转换成字符串
     *
     * @param address 地址
     * @return 地址字符串
     */
    public static String toAddress(final SocketAddress address) {
        if (address == null) {
            return null;
        }
        if (address instanceof InetSocketAddress) {
            InetSocketAddress isa = (InetSocketAddress) address;
            InetAddress inetAddress = isa.getAddress();
            String host = toIp(inetAddress);
            if (inetAddress instanceof Inet6Address) {
                return "[" + host + "]" + ':' + isa.getPort();
            }
            return host + ':' + isa.getPort();
        } else {
            return address.toString();
        }
    }

    /**
     * 获取IP字符串
     *
     * @param address 地址
     * @return IP字符串
     */
    public static String toIp(final InetAddress address) {
        String result = address == null ? null : address.getHostAddress();
        if (address instanceof Inet6Address) {
            int pos = result.lastIndexOf('%');
            if (pos > 0) {
                return result.substring(0, pos);
            }
        }
        return result;
    }

    /**
     * 得到ip
     *
     * @param address 地址
     *
     * @return ip
     */
    public static String toIp(final InetSocketAddress address) {
        if (address == null) {
            return null;
        }
        InetAddress inetAddress = address.getAddress();
        return inetAddress == null ? address.getHostName() : toIp(inetAddress);
    }

}
