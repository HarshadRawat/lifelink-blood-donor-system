package com.example.blooddonor.controller;

import com.example.blooddonor.dto.LoginRequest;
import com.example.blooddonor.dto.LoginResponse;
import com.example.blooddonor.dto.RegisterRequest;
import com.example.blooddonor.dto.UserResponse;
import com.example.blooddonor.exception.AdminRegistrationNotAllowedException;
import com.example.blooddonor.exception.EmailAlreadyRegisteredException;
import com.example.blooddonor.exception.InvalidCredentialsException;
import com.example.blooddonor.service.JwtService;
import com.example.blooddonor.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {


    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private JsonMapper objectMapper;


    @MockitoBean
    private UserService userService;


    @MockitoBean
    private JwtService jwtService;



    // =========================================================
    // 1. REGISTER - SUCCESS
    // =========================================================

    @Test
    void register_shouldReturn200_whenValidRequest() throws Exception {

        RegisterRequest request = new RegisterRequest();

        request.setName("Test Donor");
        request.setEmail("testdonor@gmail.com");
        request.setPassword("Donor123");
        request.setRole("DONOR");


        UserResponse response = new UserResponse();

        response.setId(1L);
        response.setName("Test Donor");
        response.setEmail("testdonor@gmail.com");
        response.setRole("DONOR");


        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(response);


        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Donor"))
                .andExpect(jsonPath("$.email").value("testdonor@gmail.com"))
                .andExpect(jsonPath("$.role").value("DONOR"));
    }



    // =========================================================
    // 2. REGISTER - VALIDATION FAILURE
    // =========================================================

    @Test
    void register_shouldReturn400_whenInvalidRequest() throws Exception {

        RegisterRequest request = new RegisterRequest();

        request.setName("");
        request.setEmail("");
        request.setPassword("");
        request.setRole("");


        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());
    }



    // =========================================================
    // 3. REGISTER - ADMIN REGISTRATION
    // =========================================================

    @Test
    void register_shouldReturn403_whenAdminRegistrationAttempted()
            throws Exception {

        RegisterRequest request = new RegisterRequest();

        request.setName("Admin User");
        request.setEmail("admin@gmail.com");
        request.setPassword("Admin123");
        request.setRole("ADMIN");


        when(userService.register(any(RegisterRequest.class)))
                .thenThrow( new AdminRegistrationNotAllowedException( "Admin registration is not allowed" ) );



        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isForbidden());
    }



    // =========================================================
    // 4. REGISTER - DUPLICATE EMAIL
    // =========================================================

    @Test
    void register_shouldReturn409_whenEmailAlreadyExists()
            throws Exception {

        RegisterRequest request = new RegisterRequest();

        request.setName("Test Donor");
        request.setEmail("testdonor@gmail.com");
        request.setPassword("Donor123");
        request.setRole("DONOR");


        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(
                        new EmailAlreadyRegisteredException(
                                "Email already registered"
                        )
                );


        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isConflict());
    }



    // =========================================================
    // 5. LOGIN - SUCCESS
    // =========================================================

    @Test
    void login_shouldReturn200_whenValidCredentials()
            throws Exception {

        LoginRequest request = new LoginRequest();

        request.setEmail("testdonor@gmail.com");
        request.setPassword("Donor123");


        LoginResponse response = new LoginResponse();

        response.setToken("dummy-jwt-token");


        UserResponse userResponse = new UserResponse();

        userResponse.setId(1L);
        userResponse.setName("Test Donor");
        userResponse.setEmail("testdonor@gmail.com");
        userResponse.setRole("DONOR");


        response.setUser(userResponse);


        when(userService.login(any(LoginRequest.class)))
                .thenReturn(response);


        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.token")
                                .value("dummy-jwt-token")
                )
                .andExpect(
                        jsonPath("$.user.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.user.name")
                                .value("Test Donor")
                )
                .andExpect(
                        jsonPath("$.user.email")
                                .value("testdonor@gmail.com")
                )
                .andExpect(
                        jsonPath("$.user.role")
                                .value("DONOR")
                );
    }



    // =========================================================
    // 6. LOGIN - VALIDATION FAILURE
    // =========================================================

    @Test
    void login_shouldReturn400_whenInvalidRequest()
            throws Exception {

        LoginRequest request = new LoginRequest();

        request.setEmail("");
        request.setPassword("");


        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isBadRequest());
    }



    // =========================================================
    // 7. LOGIN - INVALID CREDENTIALS
    // =========================================================

    @Test
    void login_shouldReturn401_whenInvalidCredentials()
            throws Exception {

        LoginRequest request = new LoginRequest();

        request.setEmail("testdonor@gmail.com");
        request.setPassword("WrongPassword");


        when(userService.login(any(LoginRequest.class)))
                .thenThrow(
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );


        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(request)
                                )
                )
                .andExpect(status().isUnauthorized());
    }
}