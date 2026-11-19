package com.example.blooddonor.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ClientIpResolver Unit Tests")
class ClientIpResolverTest {

    @Test
    @DisplayName("Should return remote address directly when no X-Forwarded-For header present")
    void resolve_whenNoXffHeader_shouldReturnRemoteAddr() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1,::1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.195");

        String resolvedIp = resolver.resolve(request);
        assertEquals("203.0.113.195", resolvedIp);
    }

    @Test
    @DisplayName("Should parse and trust first IP in X-Forwarded-For when request arrives from trusted proxy")
    void resolve_whenFromTrustedProxy_shouldReturnFirstXffIp() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1,::1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "198.51.100.42, 10.0.0.1");

        String resolvedIp = resolver.resolve(request);
        assertEquals("198.51.100.42", resolvedIp);
    }

    @Test
    @DisplayName("Should IGNORE forged X-Forwarded-For header when request arrives from untrusted remote address")
    void resolve_whenFromUntrustedRemote_shouldIgnoreForgedXff() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1,::1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.99"); // Untrusted external client
        request.addHeader("X-Forwarded-For", "127.0.0.1"); // Attempted spoof

        String resolvedIp = resolver.resolve(request);
        assertEquals("198.51.100.99", resolvedIp, "Spoofed X-Forwarded-For must be ignored from untrusted remotes");
    }

    @Test
    @DisplayName("Should match and trust proxy within configured CIDR subnet range")
    void resolve_whenProxyInCidrSubnet_shouldTrustXff() {
        ClientIpResolver resolver = new ClientIpResolver("172.28.0.0/16,127.0.0.1,::1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.28.0.4"); // Inside Docker bridge subnet
        request.addHeader("X-Forwarded-For", "203.0.113.55");

        String resolvedIp = resolver.resolve(request);
        assertEquals("203.0.113.55", resolvedIp);
    }

    @Test
    @DisplayName("Should ignore X-Forwarded-For when proxy remote address falls outside configured CIDR subnet")
    void resolve_whenProxyOutsideCidrSubnet_shouldIgnoreXff() {
        ClientIpResolver resolver = new ClientIpResolver("172.28.0.0/16,127.0.0.1,::1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.50"); // Outside 172.28.0.0/16
        request.addHeader("X-Forwarded-For", "203.0.113.55");

        String resolvedIp = resolver.resolve(request);
        assertEquals("192.168.1.50", resolvedIp);
    }
}
