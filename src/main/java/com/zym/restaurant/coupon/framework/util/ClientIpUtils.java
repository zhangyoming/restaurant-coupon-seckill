package com.zym.restaurant.coupon.framework.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/** 客户端 IP 工具类。 */
public final class ClientIpUtils {

    private static final String UNKNOWN = "unknown";

    private ClientIpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        String ip = firstValid(request.getHeader("X-Forwarded-For"));
        if (hasText(ip)) {
            return normalize(ip);
        }
        ip = request.getHeader("X-Real-IP");
        if (hasText(ip)) {
            return normalize(ip);
        }
        ip = request.getHeader("Proxy-Client-IP");
        if (hasText(ip)) {
            return normalize(ip);
        }
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (hasText(ip)) {
            return normalize(ip);
        }
        return normalize(request.getRemoteAddr());
    }

    private static String firstValid(String value) {
        if (!hasText(value)) {
            return null;
        }
        String[] parts = value.split(",");
        for (String part : parts) {
            if (hasText(part)) {
                return part.trim();
            }
        }
        return null;
    }

    private static boolean hasText(String value) {
        return StringUtils.hasText(value) && !UNKNOWN.equalsIgnoreCase(value.trim());
    }

    private static String normalize(String ip) {
        if (!StringUtils.hasText(ip)) {
            return "0.0.0.0";
        }
        return ip.trim().replace(':', '_');
    }
}
