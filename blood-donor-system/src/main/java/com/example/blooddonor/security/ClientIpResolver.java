package com.example.blooddonor.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class ClientIpResolver {

    private final List<IpAddressMatcher> trusted;

    public ClientIpResolver(@Value("${security.rate-limit.trusted-proxies:127.0.0.1,::1}") String cfg) {
        this.trusted = Arrays.stream(cfg.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();
    }

    public String resolve(HttpServletRequest req) {
        String remote = req.getRemoteAddr();
        String xff = req.getHeader("X-Forwarded-For");
        boolean fromProxy = trusted.stream().anyMatch(m -> m.matches(remote));
        if (fromProxy && xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return remote;
    }
}
