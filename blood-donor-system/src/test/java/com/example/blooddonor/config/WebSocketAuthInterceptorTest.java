package com.example.blooddonor.config;

import com.example.blooddonor.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(jwtService);
    }

    @Test
    @DisplayName("CONNECT frame with valid JWT should set user authentication and tokenExpiresAt in session attributes")
    void connect_withValidJwt_shouldAuthenticateAndStoreExpiry() {
        String token = "valid.jwt.token";
        Date expiresAt = Date.from(Instant.now().plus(15, ChronoUnit.MINUTES));

        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(42L);
        when(jwtService.extractRole(token)).thenReturn("DONOR");
        when(jwtService.extractExpiration(token)).thenReturn(expiresAt);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Map<String, Object> sessionAttributes = new HashMap<>();
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals("42", resultAccessor.getUser().getName());
        Authentication auth = (Authentication) resultAccessor.getUser();
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DONOR")));

        assertEquals(expiresAt.toInstant(), sessionAttributes.get("tokenExpiresAt"));
    }

    @Test
    @DisplayName("CONNECT frame with missing Authorization header should throw MessageDeliveryException")
    void connect_withoutAuthorizationHeader_shouldThrow() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        MessageDeliveryException ex = assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("Missing or invalid Authorization header"));
    }

    @Test
    @DisplayName("Subsequent frame before expiration should be permitted")
    void nonConnectFrame_beforeExpiry_shouldPass() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/user/queue/notifications");
        accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "42",
                null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_DONOR"))
        ));

        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("tokenExpiresAt", Instant.now().plus(10, ChronoUnit.MINUTES));
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);
        assertNotNull(result);
    }

    @Test
    @DisplayName("Subsequent frame mid-session after expiration should throw MessageDeliveryException forcing reconnect")
    void nonConnectFrame_afterExpiry_shouldThrowMidSessionExpiryException() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        accessor.setDestination("/app/ping");

        Map<String, Object> sessionAttributes = new HashMap<>();
        // Token expired 1 minute ago!
        sessionAttributes.put("tokenExpiresAt", Instant.now().minus(1, ChronoUnit.MINUTES));
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        MessageDeliveryException ex = assertThrows(
                MessageDeliveryException.class,
                () -> interceptor.preSend(message, messageChannel)
        );

        assertTrue(ex.getMessage().contains("JWT token has expired mid-session"));
    }
}
