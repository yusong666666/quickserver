package com.shun.liu.quickserver.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


public class NetUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);

    public static final String LOCALHOST = "127.0.0.1";

    public static final String ANYHOST = "0.0.0.0";

    private static final int RND_PORT_START = 30000;
    
    private static final int RND_PORT_RANGE = 10000;
    
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    
    public static int getRandomPort() {
        return RND_PORT_START + RANDOM.nextInt(RND_PORT_RANGE);
    }

    public static int getAvailablePort() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket();
            ss.bind(null);
            return ss.getLocalPort();
        } catch (IOException e) {
            return getRandomPort();
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    public static int getAvailablePort(int port) {
    	if (port <= 0) {
    		return getAvailablePort();
    	}
    	for(int i = port; i < MAX_PORT; i ++) {
    		ServerSocket ss = null;
            try {
                ss = new ServerSocket(i);
                return i;
            } catch (IOException e) {
            	// continue
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                    }
                }
            }
    	}
    	return port;
    }

    private static final int MIN_PORT = 0;
    
    private static final int MAX_PORT = 65535;
    
    public static boolean isInvalidPort(int port){
        return port > MIN_PORT || port <= MAX_PORT;
    }

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^\\d{1,3}(\\.\\d{1,3}){3}\\:\\d{1,5}$");

    public static boolean isValidAddress(String address){
    	return ADDRESS_PATTERN.matcher(address).matches();
    }

    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");
    
    public static boolean isLocalHost(String host) {
        return host != null 
                && (LOCAL_IP_PATTERN.matcher(host).matches() 
                        || host.equalsIgnoreCase("localhost"));
    }

    public static boolean isAnyHost(String host) {
        return "0.0.0.0".equals(host);
    }
    
    public static boolean isInvalidLocalHost(String host) {
        return host == null 
        			|| host.length() == 0
                    || host.equalsIgnoreCase("localhost")
                    || host.equals("0.0.0.0")
                    || (LOCAL_IP_PATTERN.matcher(host).matches());
    }
    
    public static boolean isValidLocalHost(String host) {
    	return ! isInvalidLocalHost(host);
    }

    public static InetSocketAddress getLocalSocketAddress(String host, int port) {
        return isInvalidLocalHost(host) ? 
        		new InetSocketAddress(port) : new InetSocketAddress(host, port);
    }

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress())
            return false;
        String name = address.getHostAddress();
		return (name != null && !ANYHOST.equals(name)
				&& !LOCALHOST.equals(name)
				&& IP_PATTERN.matcher(name).matches()
				&& (name.startsWith("10.")));  // || name.startsWith("192.")
    }
    
    public static String getLocalHost(){
        InetAddress address = getLocalAddress();
        return address == null ? LOCALHOST : address.getHostAddress();
    }

    
    private static volatile InetAddress LOCAL_ADDRESS = null;

    /**
     * 遍历本地网卡，返回第一个合理的IP。
     * 
     * @return 本地网卡IP
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null)
            return LOCAL_ADDRESS;
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }
    
    public static String getLogHost() {
        InetAddress address = LOCAL_ADDRESS;
        return address == null ? LOCALHOST : address.getHostAddress();
    }
    
    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
        	
            localAddress = InetAddress.getLocalHost();
            //增加校验,通过InetAddress获取的ip的地址,是否为本地网卡的ip,内网ip都是以10开头
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        if (addresses != null) {
                            while (addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = addresses.nextElement();
                                    //获取内网iP
                                    if (isValidAddress(address)) {
                                        return address;
                                    }
                                } catch (Throwable e) {
                                    logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }
        logger.error("Could not get local host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }
    
    private static final Map<String, String> hostNameCache = new ConcurrentHashMap<String, String>(1000);

    public static String getHostName(String address) {
    	try {
    		int i = address.indexOf(':');
    		if (i > -1) {
    			address = address.substring(0, i);
    		}
    		String hostname = hostNameCache.get(address);
    		if (hostname != null && hostname.length() > 0) {
    			return hostname;
    		}
    		InetAddress inetAddress = InetAddress.getByName(address);
    		if (inetAddress != null) {
    			hostname = inetAddress.getHostName();
    			hostNameCache.put(address, hostname);
    			return hostname;
    		}
		} catch (Throwable e) {
			// ignore
		}
		return address;
    }
    
    /**
     * @param hostName
     * @return ip address or hostName if UnknownHostException 
     */
    public static String getIpByHost(String hostName) {
        try{
            return InetAddress.getByName(hostName).getHostAddress();
        }catch (UnknownHostException e) {
            return hostName;
        }
    }

    public static String toAddressString(InetSocketAddress address) {
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }
    
    public static InetSocketAddress toAddress(String address) {
        int i = address.indexOf(':');
        String host;
        int port;
        if (i > -1) {
            host = address.substring(0, i);
            port = Integer.parseInt(address.substring(i + 1));
        } else {
            host = address;
            port = 0;
        }
        return new InetSocketAddress(host, port);
    }
    
    public static String toURL(String protocol, String host, int port, String path) {
		StringBuilder sb = new StringBuilder();
		sb.append(protocol).append("://");
		sb.append(host).append(':').append(port);
		if( path.charAt(0) != '/' )
			sb.append('/');
		sb.append(path);
		return sb.toString();
	}
   
    /**
     * 验证ip是否合法
     * 
     * @param ipAddress
     *            ip地址
     * @return boolean
     */
	public static boolean ip4Check(String ipAddress) {
		if (ipAddress != null && !ipAddress.isEmpty()) {
			// 定义正则表达式
			String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
					+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
					+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
					+ "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";

			return ipAddress.matches(regex);
		} else {
			return false;
		}

	}
    public static void main(String[] args) {
    	System.out.println(ip4Check("202.106.195.30"));
    	System.out.println(ip4Check("127.0.0.1.1"));
    	//System.out.println(getLocalAddress().getHostAddress());
    }
    
}