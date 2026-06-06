package app.keystone.common.utils.ip;

import app.keystone.common.config.KeystoneConfig;
import app.keystone.common.utils.jackson.JacksonUtil;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import lombok.extern.slf4j.Slf4j;

/**
 * query geography address from ip
 *
 * @author likanug
 */
@Slf4j
public class OnlineIpRegionUtil {

    private OnlineIpRegionUtil() {
    }

    /**
     * website for query geography address from ip
     */
    public static final String ADDRESS_QUERY_SITE = "http://whois.pconline.com.cn/ipJson.jsp";


    public static IpRegion getIpRegion(String ip) {
        if (ip == null || ip.trim().isEmpty() || IpUtil.isValidIpv6(ip) || !IpUtil.isValidIpv4(ip)) {
            return null;
        }

        if (KeystoneConfig.isAddressEnabled()) {
            try {
                String rspStr = fetchIpRegion(ip);

                if (rspStr == null || rspStr.isEmpty()) {
                    log.error("获取地理位置异常 {}", ip);
                    return null;
                }

                String province = JacksonUtil.getAsString(rspStr, "pro");
                String city = JacksonUtil.getAsString(rspStr, "city");
                return new IpRegion(province, city);
            } catch (Exception e) {
                log.error("获取地理位置异常 {}", ip);
            }
        }
        return null;
    }

    private static String fetchIpRegion(String ip) throws Exception {
        String url = ADDRESS_QUERY_SITE + "?ip=" + URLEncoder.encode(ip, "UTF-8") + "&json=true";
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        connection.setDoInput(true);

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), Charset.forName("GBK")))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

}
