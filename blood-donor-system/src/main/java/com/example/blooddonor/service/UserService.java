package com.example.blooddonor.service;

import com.example.blooddonor.dto.LoginRequest;
import com.example.blooddonor.dto.LoginResponse;
import com.example.blooddonor.dto.RegisterRequest;
import com.example.blooddonor.dto.UserResponse;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.exception.AdminRegistrationNotAllowedException;
import com.example.blooddonor.exception.EmailAlreadyRegisteredException;
import com.example.blooddonor.exception.InvalidCredentialsException;
import com.example.blooddonor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final java.time.Clock clock;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this(userRepository, passwordEncoder, jwtService, null, java.time.Clock.systemDefaultZone());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       java.time.Clock clock) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.clock = clock != null ? clock : java.time.Clock.systemDefaultZone();
    }

    public UserResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {

            throw new EmailAlreadyRegisteredException(
                    "Email already registered"
            );
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        user.setPassword(hashedPassword);

        if ("ADMIN".equalsIgnoreCase(request.getRole())) {

            throw new AdminRegistrationNotAllowedException(
                    "Admin registration is not allowed"
            );
        }

        Role role = Role.valueOf(
                request.getRole().toUpperCase()
        );

        user.setRole(role);

        User savedUser = userRepository.save(user);

        UserResponse response = new UserResponse();

        response.setId(savedUser.getId());
        response.setName(savedUser.getName());
        response.setEmail(savedUser.getEmail());
        response.setRole(savedUser.getRole().name());

        return response;
    }

    public LoginResponse login(LoginRequest request) {

        // 1. Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Authentication failure - unknown user: emailHash={}, correlationId={}",
                            hashEmail(request.getEmail()), MDC.get("correlationId"));
                    return new InvalidCredentialsException("Invalid email or password");
                });

        // 2. Check account lockout status (Brute-Force Protection - Task 9)
        java.time.LocalDateTime now = java.time.LocalDateTime.now(clock);
        if (user.getAccountLockedUntil() != null) {
            if (user.getAccountLockedUntil().isAfter(now)) {
                long minutesRemaining = java.time.Duration.between(now, user.getAccountLockedUntil()).toMinutes() + 1;
                log.warn("Authentication blocked - account locked: userId={}, correlationId={}",
                        user.getId(), MDC.get("correlationId"));
                throw new com.example.blooddonor.exception.BusinessRuleException(
                        "Account is locked due to repeated failed logins. Please try again in " + minutesRemaining + " minutes."
                );
            } else {
                // Lock expired: reset lockout status
                user.setAccountLockedUntil(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
        }

        // 3. Verify password using BCrypt
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= 5) {
                user.setAccountLockedUntil(now.plusMinutes(15));
                userRepository.save(user);
                log.warn("Authentication failure - account locked after {} failed attempts: userId={}, correlationId={}",
                        attempts, user.getId(), MDC.get("correlationId"));
                throw new com.example.blooddonor.exception.BusinessRuleException(
                        "Account is locked due to repeated failed logins. Please try again in 15 minutes."
                );
            }

            userRepository.save(user);
            log.warn("Authentication failure - invalid password: userId={}, attempt={}/5, correlationId={}",
                    user.getId(), attempts, MDC.get("correlationId"));

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        // Reset failed login attempts on successful login
        if ((user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) || user.getAccountLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }

        // 4. Generate short-lived JWT access token
        String token = jwtService.generateToken(
                user.getId(),
                user.getRole().name()
        );

        // 5. Generate refresh token with rotation (Task 9)
        String refreshToken = null;
        if (refreshTokenService != null) {
            refreshToken = refreshTokenService.createRefreshToken(user);
        }

        // 6. Create user response
        UserResponse userResponse = new UserResponse();

        userResponse.setId(user.getId());
        userResponse.setName(user.getName());
        userResponse.setEmail(user.getEmail());
        userResponse.setRole(user.getRole().name());

        // 7. Create login response
        LoginResponse response = new LoginResponse();

        response.setToken(token);
        response.setRefreshToken(refreshToken);
        response.setUser(userResponse);

        return response;
    }

    public LoginResponse refresh(com.example.blooddonor.dto.RefreshTokenRequest request) {
        if (refreshTokenService == null) {
            throw new IllegalStateException("RefreshTokenService not configured");
        }
        return refreshTokenService.rotateRefreshToken(request.getRefreshToken());
    }

    public void logout(com.example.blooddonor.dto.RefreshTokenRequest request) {
        if (refreshTokenService != null && request != null) {
            refreshTokenService.revokeToken(request.getRefreshToken());
        }
    }

    private static String hashEmail(String email) {
        if (email == null) return "null";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(email.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (Exception e) {
            return "hash_error";
        }
    }
}