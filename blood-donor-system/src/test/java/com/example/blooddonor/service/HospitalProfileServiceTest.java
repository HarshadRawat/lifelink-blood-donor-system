package com.example.blooddonor.service;

import com.example.blooddonor.dto.HospitalProfileRequest;
import com.example.blooddonor.dto.HospitalProfileResponse;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.HospitalProfileRepository;
import com.example.blooddonor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HospitalProfileServiceTest {

    @Mock
    private HospitalProfileRepository hospitalProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private HospitalProfileService hospitalProfileService;


    // =========================================================
    // TEST #38
    // =========================================================

    @Test
    void shouldCreateHospitalProfileSuccessfully() {

        User user = new User();
        user.setId(8L);
        user.setName("Test Hospital User");

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        request.setHospitalName("Test Hospital");
        request.setRegistrationNumber("HOSP001");
        request.setAddress("Whitefield");
        request.setCity("Bangalore");
        request.setLatitude(12.9698);
        request.setLongitude(77.7499);
        request.setContactNumber("9876543210");

        when(userRepository.findById(8L))
                .thenReturn(Optional.of(user));

        when(hospitalProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());

        when(hospitalProfileRepository.save(any(HospitalProfile.class)))
                .thenAnswer(invocation -> {

                    HospitalProfile profile =
                            invocation.getArgument(0);

                    profile.setId(3L);

                    return profile;
                });

        HospitalProfileResponse response =
                hospitalProfileService.createProfile(
                        request,
                        8L
                );

        assertEquals(3L, response.getId());
        assertEquals(8L, response.getUserId());
        assertEquals(
                "Test Hospital",
                response.getHospitalName()
        );
        assertEquals(
                "HOSP001",
                response.getRegistrationNumber()
        );
        assertEquals(
                "Whitefield",
                response.getAddress()
        );
        assertEquals(
                "Bangalore",
                response.getCity()
        );
        assertEquals(
                12.9698,
                response.getLatitude()
        );
        assertEquals(
                77.7499,
                response.getLongitude()
        );
        assertEquals(
                "9876543210",
                response.getContactNumber()
        );
    }


    // =========================================================
    // TEST #39
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileCreatingProfile() {

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        request.setHospitalName("Test Hospital");
        request.setRegistrationNumber("HOSP001");
        request.setAddress("Whitefield");
        request.setCity("Bangalore");
        request.setLatitude(12.9698);
        request.setLongitude(77.7499);
        request.setContactNumber("9876543210");

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> hospitalProfileService.createProfile(
                        request,
                        99L
                )
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #40
    // =========================================================

    @Test
    void shouldRejectDuplicateHospitalProfile() {

        User user = new User();
        user.setId(8L);

        HospitalProfile existingProfile =
                new HospitalProfile();

        existingProfile.setId(3L);
        existingProfile.setUser(user);

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        request.setHospitalName("Test Hospital");
        request.setRegistrationNumber("HOSP001");
        request.setAddress("Whitefield");
        request.setCity("Bangalore");
        request.setLatitude(12.9698);
        request.setLongitude(77.7499);
        request.setContactNumber("9876543210");

        when(userRepository.findById(8L))
                .thenReturn(Optional.of(user));

        when(hospitalProfileRepository.findByUser(user))
                .thenReturn(Optional.of(existingProfile));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> hospitalProfileService.createProfile(
                        request,
                        8L
                )
        );

        assertEquals(
                "Hospital profile already exists",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #41
    // =========================================================

    @Test
    void shouldGetHospitalProfileSuccessfully() {

        User user = new User();
        user.setId(8L);

        HospitalProfile profile =
                new HospitalProfile();

        profile.setId(3L);
        profile.setUser(user);
        profile.setHospitalName("Test Hospital");
        profile.setRegistrationNumber("HOSP001");
        profile.setAddress("Whitefield");
        profile.setCity("Bangalore");
        profile.setLatitude(12.9698);
        profile.setLongitude(77.7499);
        profile.setContactNumber("9876543210");

        when(userRepository.findById(8L))
                .thenReturn(Optional.of(user));

        when(hospitalProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile));

        HospitalProfileResponse response =
                hospitalProfileService.getProfile(8L);

        assertEquals(3L, response.getId());
        assertEquals(8L, response.getUserId());
        assertEquals(
                "Test Hospital",
                response.getHospitalName()
        );
        assertEquals(
                "HOSP001",
                response.getRegistrationNumber()
        );
        assertEquals(
                "Whitefield",
                response.getAddress()
        );
        assertEquals(
                "Bangalore",
                response.getCity()
        );
        assertEquals(
                12.9698,
                response.getLatitude()
        );
        assertEquals(
                77.7499,
                response.getLongitude()
        );
        assertEquals(
                "9876543210",
                response.getContactNumber()
        );
    }


    // =========================================================
    // TEST #42
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileGettingProfile() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> hospitalProfileService.getProfile(99L)
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #43
    // =========================================================

    @Test
    void shouldThrowExceptionWhenHospitalProfileNotFoundWhileGettingProfile() {

        User user = new User();
        user.setId(8L);

        when(userRepository.findById(8L))
                .thenReturn(Optional.of(user));

        when(hospitalProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> hospitalProfileService.getProfile(8L)
        );

        assertEquals(
                "Hospital profile not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #44
    // =========================================================

    @Test
    void shouldUpdateHospitalProfileSuccessfully() {

        User user = new User();
        user.setId(8L);

        HospitalProfile profile =
                new HospitalProfile();

        profile.setId(3L);
        profile.setUser(user);

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        request.setHospitalName("Updated Hospital");
        request.setRegistrationNumber("HOSP002");
        request.setAddress("Electronic City");
        request.setCity("Bangalore");
        request.setLatitude(12.8456);
        request.setLongitude(77.6603);
        request.setContactNumber("9999999999");

        when(userRepository.findById(8L))
                .thenReturn(Optional.of(user));

        when(hospitalProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile));

        when(hospitalProfileRepository.save(profile))
                .thenReturn(profile);

        HospitalProfileResponse response =
                hospitalProfileService.updateProfile(
                        request,
                        8L
                );

        assertEquals(3L, response.getId());
        assertEquals(8L, response.getUserId());
        assertEquals(
                "Updated Hospital",
                response.getHospitalName()
        );
        assertEquals(
                "HOSP002",
                response.getRegistrationNumber()
        );
        assertEquals(
                "Electronic City",
                response.getAddress()
        );
        assertEquals(
                "Bangalore",
                response.getCity()
        );
        assertEquals(
                12.8456,
                response.getLatitude()
        );
        assertEquals(
                77.6603,
                response.getLongitude()
        );
        assertEquals(
                "9999999999",
                response.getContactNumber()
        );
    }


    // =========================================================
    // TEST #45
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileUpdatingProfile() {

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> hospitalProfileService.updateProfile(
                        request,
                        99L
                )
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #46
    // =========================================================

    @Test
    void shouldThrowExceptionWhenHospitalProfileNotFoundWhileUpdatingProfile() {

        User user = new User();
        user.setId(8L);

        HospitalProfileRequest request =
                new HospitalProfileRequest();

        when(userRepository.findById(8L))
                .thenReturn(Optional.of(user));

        when(hospitalProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> hospitalProfileService.updateProfile(
                        request,
                        8L
                )
        );

        assertEquals(
                "Hospital profile not found",
                exception.getMessage()
        );
    }
}