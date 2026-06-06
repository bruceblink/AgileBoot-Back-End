package app.keystone.common.utils.ip;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * ip校验器
 *
 * @author likanug
 */
@Slf4j
public class IpUtil {

    public static final String INNER_IP_REGEX = "^(127\\.0\\.0\\.\\d{1,3})|(localhost)|(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(172\\.((1[6-9])|(2\\d)|(3[01]))\\.\\d{1,3}\\.\\d{1,3})|(192\\.168\\.\\d{1,3}\\.\\d{1,3})$";
    public static final Pattern INNER_IP_PATTERN = Pattern.compile(INNER_IP_REGEX);

    private IpUtil() {
    }

    public static boolean isInnerIp(String ip) {
        return INNER_IP_PATTERN.matcher(ip).matches() || isLocalHost(ip);
    }

    public static boolean isLocalHost(String ipAddress) {
        InetAddress ia = null;
        try {
            InetAddress ad = InetAddress.getByName(ipAddress);
            byte[] ip = ad.getAddress();
            ia = InetAddress.getByAddress(ip);
        } catch (UnknownHostException e) {
            log.error("解析Ip失败", e);
        }
        if (ia == null) {
            return false;
        }
        return ia.isSiteLocalAddress() || ia.isLoopbackAddress();
    }


    public static boolean isValidIpv4(String inetAddress) {
        if (inetAddress == null || inetAddress.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(inetAddress);
            return address instanceof Inet4Address && inetAddress.equals(address.getHostAddress());
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidIpv6(String inetAddress) {
        if (inetAddress == null || inetAddress.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(inetAddress);
            return address instanceof Inet6Address;
        } catch (Exception e) {
            return false;
        }
    }

}
