package com.example.blooddonor.config;

import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@lifelink.com}")
    private String adminEmail;

    @Value("${app.admin.password:Admin123}")
    private String adminPassword;

    public AdminUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = new User();
            admin.setName("LifeLink System Admin");
            admin.setEmail(adminEmail);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);
            log.info("Initialized default administrative account: {}", adminEmail);
        }
    }
}
