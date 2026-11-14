package com.example.blooddonor.config;

import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoDataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HospitalProfileRepository hospitalProfileRepository;

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private RequestDonorMatchRepository requestDonorMatchRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Environment environment;

    private DemoDataInitializer demoDataInitializer;

    @BeforeEach
    void setUp() {
        demoDataInitializer = new DemoDataInitializer(
                userRepository,
                hospitalProfileRepository,
                donorProfileRepository,
                emergencyRequestRepository,
                requestDonorMatchRepository,
                notificationRepository,
                passwordEncoder,
                environment
        );
    }

    @Test
    @DisplayName("Should skip seeding when neither demo profile nor app.demo.seed is enabled")
    void shouldSkipSeeding_whenDisabled() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ReflectionTestUtils.setField(demoDataInitializer, "demoSeedEnabled", false);

        demoDataInitializer.run();

        verifyNoInteractions(userRepository);
        verifyNoInteractions(hospitalProfileRepository);
    }

    @Test
    @DisplayName("Should skip seeding if demo hospital already exists (idempotent)")
    void shouldSkipSeeding_whenAlreadyInitialized() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"demo"});
        ReflectionTestUtils.setField(demoDataInitializer, "demoSeedEnabled", false);
        when(userRepository.findByEmail("hospital@lifelink.com")).thenReturn(Optional.of(new User()));

        demoDataInitializer.run();

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(hospitalProfileRepository);
    }

    @Test
    @DisplayName("Should seed complete demo cluster when enabled and not yet initialized")
    void shouldSeedCompleteCluster_whenEnabledAndNotInitialized() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"demo"});
        ReflectionTestUtils.setField(demoDataInitializer, "demoSeedEnabled", false);
        when(userRepository.findByEmail("hospital@lifelink.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pwd");

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(hospitalProfileRepository.save(any(HospitalProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(donorProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(emergencyRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        demoDataInitializer.run();

        // 1 Hospital + 5 Donors = 6 Users saved
        verify(userRepository, times(6)).save(any(User.class));
        verify(hospitalProfileRepository, times(1)).save(any(HospitalProfile.class));
        verify(donorProfileRepository, times(5)).save(any());
        verify(emergencyRequestRepository, times(1)).save(any());
        verify(requestDonorMatchRepository, times(2)).save(any());
        verify(notificationRepository, times(2)).save(any());
    }
}
