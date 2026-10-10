package com.example.blooddonor.service;

import com.example.blooddonor.dto.LoginRequest;
import com.example.blooddonor.dto.LoginResponse;
import com.example.blooddonor.dto.RegisterRequest;
import com.example.blooddonor.dto.UserResponse;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
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
}