
package com.example.blooddonor.config;

import com.example.blooddonor.service.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    public WebSocketAuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel
    ) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class
                );

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            String authorization =
                    accessor.getFirstNativeHeader("Authorization");

            if (authorization == null ||
                    !authorization.startsWith("Bearer ")) {
                throw new MessageDeliveryException(
                        "Missing or invalid Authorization header"
                );
            }

            String token = authorization.substring(7);

            try {
                if (!jwtService.isTokenValid(token)) {
                    throw new MessageDeliveryException(
                            "Invalid JWT token"
                    );
                }

                Long userId = jwtService.extractUserId(token);
                String role = jwtService.extractRole(token);

                if (role == null || role.isBlank()) {
                    throw new MessageDeliveryException(
                            "Invalid role in JWT token"
                    );
                }

                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                String.valueOf(userId),
                                null,
                                List.of(
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                        );

                accessor.setUser(authentication);

            } catch (MessageDeliveryException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new MessageDeliveryException(
                        "Unable to authenticate WebSocket connection: "
                                + exception.getMessage()
                );
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {

            Authentication authentication =
                    (Authentication) accessor.getUser();

            if (authentication == null ||
                    !authentication.isAuthenticated()) {
                throw new MessageDeliveryException(
                        "Authentication is required to subscribe"
                );
            }

            String destination = accessor.getDestination();

            if (!"/user/queue/notifications".equals(destination)) {
                throw new MessageDeliveryException(
                        "Subscription to this destination is not allowed"
                );
            }
        }

        return message;
    }
}