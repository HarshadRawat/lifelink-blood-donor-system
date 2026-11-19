package com.example.blooddonor.service;

import com.example.blooddonor.dto.LoginRequest;
import com.example.blooddonor.dto.LoginResponse;
import com.example.blooddonor.dto.RegisterRequest;
import com.example.blooddonor.dto.UserResponse;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.exception.BusinessRuleException;
import com.example.blooddonor.exception.InvalidCredentialsException;
import com.example.blooddonor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;


    @Test
    void shouldRegisterDonorSuccessfully() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Test Donor");
        request.setEmail("donor@test.com");
        request.setPassword("Donor123");
        request.setRole("DONOR");


        // Email does not already exist
        when(userRepository.findByEmail("donor@test.com"))
                .thenReturn(Optional.empty());


        // Password encoding
        when(passwordEncoder.encode("Donor123"))
                .thenReturn("encodedPassword");


        // Saved user
        User savedUser = new User();

        savedUser.setId(1L);
        savedUser.setName("Test Donor");
        savedUser.setEmail("donor@test.com");
        savedUser.setPassword("encodedPassword");
        savedUser.setRole(Role.DONOR);


        when(userRepository.save(any(User.class)))
                .thenReturn(savedUser);


        UserResponse result =
                userService.register(request);


        assertEquals(1L, result.getId());
        assertEquals("Test Donor", result.getName());
        assertEquals("donor@test.com", result.getEmail());
        assertEquals("DONOR", result.getRole());


        verify(userRepository)
                .save(any(User.class));

        verify(passwordEncoder)
                .encode("Donor123");
    }

    @Test
    void shouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Another Donor");
        request.setEmail("donor@test.com");
        request.setPassword("Donor123");
        request.setRole("DONOR");


        User existingUser = new User();

        existingUser.setId(1L);
        existingUser.setEmail("donor@test.com");
        existingUser.setRole(Role.DONOR);


        when(userRepository.findByEmail("donor@test.com"))
                .thenReturn(Optional.of(existingUser));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(request)
        );


        assertEquals(
                "Email already registered",
                exception.getMessage()
        );


        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(anyString());
    }

    @Test
    void shouldRejectAdminRegistration() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Admin User");
        request.setEmail("admin@test.com");
        request.setPassword("Admin123");
        request.setRole("ADMIN");


        when(userRepository.findByEmail("admin@test.com"))
                .thenReturn(Optional.empty());


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.register(request)
        );


        assertEquals(
                "Admin registration is not allowed",
                exception.getMessage()
        );


        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldRejectInvalidRole() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Test User");
        request.setEmail("invalid@test.com");
        request.setPassword("Test123");
        request.setRole("INVALID");


        when(userRepository.findByEmail("invalid@test.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("Test123"))
                .thenReturn("encodedPassword");


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(request)
        );


        assertEquals(
                "No enum constant com.example.blooddonor.entity.Role.INVALID",
                exception.getMessage()
        );


        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldSaveEncodedPassword() {

        RegisterRequest request = new RegisterRequest();

        request.setName("Password Test");
        request.setEmail("password@test.com");
        request.setPassword("PlainPassword123");
        request.setRole("DONOR");


        when(userRepository.findByEmail("password@test.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("PlainPassword123"))
                .thenReturn("encodedPassword");


        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));


        userService.register(request);


        verify(userRepository).save(
                argThat(user ->
                        user.getPassword().equals("encodedPassword")
                                && !user.getPassword()
                                .equals("PlainPassword123")
                )
        );
    }

    @Test
    void shouldLoginSuccessfully() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("Test123");

        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.DONOR);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("Test123", "encodedPassword"))
                .thenReturn(true);

        when(jwtService.generateToken(1L, "DONOR"))
                .thenReturn("jwt-token");


        LoginResponse response = userService.login(request);


        assertEquals("jwt-token", response.getToken());

        assertEquals(1L, response.getUser().getId());
        assertEquals("Test User", response.getUser().getName());
        assertEquals("test@gmail.com", response.getUser().getEmail());
        assertEquals("DONOR", response.getUser().getRole());
    }

    @Test
    void shouldRejectLoginWhenEmailNotFound() {

        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@gmail.com");
        request.setPassword("Test123");

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(passwordEncoder, never())
                .matches(anyString(), anyString());

        verify(jwtService, never())
                .generateToken(anyLong(), anyString());
    }

    @Test
    void shouldRejectLoginWhenPasswordIsWrong() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@gmail.com");
        request.setPassword("WrongPassword");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.DONOR);

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("WrongPassword", "encodedPassword"))
                .thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> userService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(jwtService, never())
                .generateToken(anyLong(), anyString());
    }

    @Test
    void shouldLockAccountAfterFiveConsecutiveFailedLogins() {
        LoginRequest request = new LoginRequest();
        request.setEmail("lockme@gmail.com");
        request.setPassword("WrongPassword");

        User user = new User();
        user.setId(10L);
        user.setEmail("lockme@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.DONOR);
        user.setFailedLoginAttempts(4); // 4 prior failures

        when(userRepository.findByEmail("lockme@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> userService.login(request)
        );

        assertEquals(5, user.getFailedLoginAttempts());
        org.junit.jupiter.api.Assertions.assertNotNull(user.getAccountLockedUntil());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("Account is locked"));
        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectLoginWhenAccountIsCurrentlyLocked() {
        LoginRequest request = new LoginRequest();
        request.setEmail("locked@gmail.com");
        request.setPassword("Password123");

        User user = new User();
        user.setId(11L);
        user.setEmail("locked@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.DONOR);
        user.setAccountLockedUntil(java.time.LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("locked@gmail.com")).thenReturn(Optional.of(user));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> userService.login(request)
        );

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("Account is locked"));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldResetFailedLoginAttemptsOnSuccessfulLogin() {
        LoginRequest request = new LoginRequest();
        request.setEmail("reset@gmail.com");
        request.setPassword("GoodPassword123");

        User user = new User();
        user.setId(12L);
        user.setName("Reset User");
        user.setEmail("reset@gmail.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.DONOR);
        user.setFailedLoginAttempts(3);

        when(userRepository.findByEmail("reset@gmail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("GoodPassword123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(12L, "DONOR")).thenReturn("valid-jwt");

        LoginResponse response = userService.login(request);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        assertEquals(0, user.getFailedLoginAttempts());
        org.junit.jupiter.api.Assertions.assertNull(user.getAccountLockedUntil());
        verify(userRepository).save(user);
    }

    @Test
    void shouldDeleteCurrentUserSuccessfully() {
        Long userId = 42L;
        User user = new User();
        user.setId(userId);
        user.setEmail("delete@test.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.deleteCurrentUser(userId);

        verify(refreshTokenService).revokeAllForUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentUser() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(com.example.blooddonor.exception.ResourceNotFoundException.class, () ->
                userService.deleteCurrentUser(userId)
        );
        verify(userRepository, never()).delete(any());
    }
}