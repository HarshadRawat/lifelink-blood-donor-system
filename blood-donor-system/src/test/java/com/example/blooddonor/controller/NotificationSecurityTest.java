package com.example.blooddonor.controller;

import com.example.blooddonor.service.JwtService;
import com.example.blooddonor.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void requestWithoutJwtReturns401() throws Exception {

        mockMvc.perform(
                        get("/api/notifications")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

    @Test
    void donorCannotMarkAnotherUsersNotificationAsRead()
            throws Exception {

        String donorToken =
                jwtService.generateToken(14L, "DONOR");

        when(notificationService.markAsRead(47L, 14L))
                .thenThrow(
                        new com.example.blooddonor.exception
                                .NotificationAccessDeniedException(
                                "You are not allowed to access this notification"
                        )
                );

        mockMvc.perform(
                        patch("/api/notifications/47/read")
                                .header(
                                        "Authorization",
                                        "Bearer " + donorToken
                                )
                )
                .andExpect(status().isForbidden());

        verify(notificationService).markAsRead(47L, 14L);
    }

    @Test
    void hospitalCannotMarkDonorNotificationAsRead()
            throws Exception {

        String hospitalToken =
                jwtService.generateToken(8L, "HOSPITAL");

        when(notificationService.markAsRead(47L, 8L))
                .thenThrow(
                        new com.example.blooddonor.exception
                                .NotificationAccessDeniedException(
                                "You are not allowed to access this notification"
                        )
                );

        mockMvc.perform(
                        patch("/api/notifications/47/read")
                                .header(
                                        "Authorization",
                                        "Bearer " + hospitalToken
                                )
                )
                .andExpect(status().isForbidden());

        verify(notificationService).markAsRead(47L, 8L);
    }

    @Test
    void requestWithExpiredJwtReturns401() throws Exception {

        String expiredToken =
                jwtService.generateExpiredToken(8L, "HOSPITAL");

        mockMvc.perform(
                        get("/api/notifications")
                                .header(
                                        "Authorization",
                                        "Bearer " + expiredToken
                                )
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(notificationService);
    }

}