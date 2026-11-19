package com.example.blooddonor.controller;

import com.example.blooddonor.exception.ResourceNotFoundException;
import com.example.blooddonor.service.JwtService;
import com.example.blooddonor.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Account Deletion Tests")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @DisplayName("DELETE /api/users/me should return 204 No Content for authenticated user")
    void shouldDeleteAccountSuccessfully() throws Exception {
        Long userId = 42L;
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

        doNothing().when(userService).deleteCurrentUser(userId);

        mockMvc.perform(delete("/api/users/me")
                        .principal(auth))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteCurrentUser(userId);
    }

    @Test
    @DisplayName("DELETE /api/users/me should return 404 when user is not found")
    void shouldReturn404WhenUserNotFound() throws Exception {
        Long userId = 999L;
        Authentication auth = new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

        doThrow(new ResourceNotFoundException("User not found with id: " + userId))
                .when(userService).deleteCurrentUser(userId);

        mockMvc.perform(delete("/api/users/me")
                        .principal(auth))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).deleteCurrentUser(userId);
    }
}
