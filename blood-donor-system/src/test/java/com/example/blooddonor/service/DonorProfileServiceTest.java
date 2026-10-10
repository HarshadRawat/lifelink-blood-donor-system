package com.example.blooddonor.service;

import com.example.blooddonor.dto.AvailabilityRequest;
import com.example.blooddonor.dto.DonorProfileRequest;
import com.example.blooddonor.dto.DonorProfileResponse;
import com.example.blooddonor.entity.DonorProfile;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.DonorProfileRepository;
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
public class DonorProfileServiceTest {

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DonorProfileService donorProfileService;


    // =========================================================
    // TEST 1
    // =========================================================

    @Test
    void shouldCreateDonorProfileSuccessfully() {

        User user = new User();
        user.setId(9L);
        user.setName("Test Donor");

        DonorProfileRequest request = new DonorProfileRequest();
        request.setBloodGroup("O+");
        request.setAddress("Whitefield");
        request.setCity("Bangalore");
        request.setLatitude(12.9698);
        request.setLongitude(77.7499);
        request.setContactNumber("9876543210");
        request.setAvailable(true);

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.save(any(DonorProfile.class)))
                .thenAnswer(invocation -> {

                    DonorProfile profile =
                            invocation.getArgument(0);

                    profile.setId(1L);

                    return profile;
                });

        DonorProfileResponse response =
                donorProfileService.createProfile(request, 9L);

        assertEquals(1L, response.getId());
        assertEquals(9L, response.getUserId());
        assertEquals("O+", response.getBloodGroup());
        assertEquals("Whitefield", response.getAddress());
        assertEquals("Bangalore", response.getCity());
        assertEquals(12.9698, response.getLatitude());
        assertEquals(77.7499, response.getLongitude());
        assertEquals("9876543210", response.getContactNumber());
        assertEquals(true, response.isAvailable());
    }


    // =========================================================
    // TEST 2
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileCreatingProfile() {

        DonorProfileRequest request = new DonorProfileRequest();

        request.setBloodGroup("O+");
        request.setAddress("Whitefield");
        request.setCity("Bangalore");
        request.setLatitude(12.9698);
        request.setLongitude(77.7499);
        request.setContactNumber("9876543210");
        request.setAvailable(true);

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorProfileService.createProfile(request, 99L)
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST 3
    // =========================================================

    @Test
    void shouldUpdateDonorProfileSuccessfully() {

        User user = new User();
        user.setId(9L);

        DonorProfile profile = new DonorProfile();
        profile.setId(1L);
        profile.setUser(user);

        DonorProfileRequest request = new DonorProfileRequest();

        request.setBloodGroup("A+");
        request.setAddress("Electronic City");
        request.setCity("Bangalore");
        request.setLatitude(12.8456);
        request.setLongitude(77.6603);
        request.setContactNumber("9999999999");
        request.setAvailable(false);

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile));

        when(donorProfileRepository.save(profile))
                .thenReturn(profile);

        DonorProfileResponse response =
                donorProfileService.updateProfile(request, 9L);

        assertEquals(1L, response.getId());
        assertEquals(9L, response.getUserId());
        assertEquals("A+", response.getBloodGroup());
        assertEquals("Electronic City", response.getAddress());
        assertEquals("Bangalore", response.getCity());
        assertEquals(12.8456, response.getLatitude());
        assertEquals(77.6603, response.getLongitude());
        assertEquals("9999999999", response.getContactNumber());
        assertEquals(false, response.isAvailable());
    }


    // =========================================================
    // TEST 4
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileUpdatingProfile() {

        DonorProfileRequest request = new DonorProfileRequest();

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorProfileService.updateProfile(request, 99L)
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST 5
    // =========================================================

    @Test
    void shouldThrowExceptionWhenDonorProfileNotFoundWhileUpdatingProfile() {

        User user = new User();
        user.setId(9L);

        DonorProfileRequest request = new DonorProfileRequest();

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorProfileService.updateProfile(request, 9L)
        );

        assertEquals(
                "Donor profile not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST 6
    // =========================================================

    @Test
    void shouldGetDonorProfileSuccessfully() {

        User user = new User();
        user.setId(9L);

        DonorProfile profile = new DonorProfile();

        profile.setId(1L);
        profile.setUser(user);
        profile.setBloodGroup("O+");
        profile.setAddress("Whitefield");
        profile.setCity("Bangalore");
        profile.setLatitude(12.9698);
        profile.setLongitude(77.7499);
        profile.setContactNumber("9876543210");
        profile.setAvailable(true);

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile));

        DonorProfileResponse response =
                donorProfileService.getProfile(9L);

        assertEquals(1L, response.getId());
        assertEquals(9L, response.getUserId());
        assertEquals("O+", response.getBloodGroup());
        assertEquals("Whitefield", response.getAddress());
        assertEquals("Bangalore", response.getCity());
        assertEquals(12.9698, response.getLatitude());
        assertEquals(77.7499, response.getLongitude());
        assertEquals("9876543210", response.getContactNumber());
        assertEquals(true, response.isAvailable());
    }


    // =========================================================
    // TEST 7
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileGettingProfile() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorProfileService.getProfile(99L)
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST 8
    // =========================================================

    @Test
    void shouldThrowExceptionWhenDonorProfileNotFoundWhileGettingProfile() {

        User user = new User();
        user.setId(9L);

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorProfileService.getProfile(9L)
        );

        assertEquals(
                "Donor profile not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST 9
    // =========================================================

    @Test
    void shouldUpdateAvailabilitySuccessfully() {

        User user = new User();
        user.setId(9L);

        DonorProfile profile = new DonorProfile();

        profile.setId(1L);
        profile.setUser(user);
        profile.setBloodGroup("O+");
        profile.setAddress("Whitefield");
        profile.setCity("Bangalore");
        profile.setLatitude(12.9698);
        profile.setLongitude(77.7499);
        profile.setContactNumber("9876543210");
        profile.setAvailable(true);

        AvailabilityRequest request =
                new AvailabilityRequest();

        request.setAvailable(false);

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUser(user))
                .thenReturn(Optional.of(profile));

        when(donorProfileRepository.save(profile))
                .thenReturn(profile);

        DonorProfileResponse response =
                donorProfileService.updateAvailability(
                        request,
                        9L
                );

        assertEquals(1L, response.getId());
        assertEquals(9L, response.getUserId());
        assertEquals("O+", response.getBloodGroup());
        assertEquals(false, response.isAvailable());
    }


    // =========================================================
    // TEST 10
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileUpdatingAvailability() {

        AvailabilityRequest request =
                new AvailabilityRequest();

        request.setAvailable(false);

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorProfileService.updateAvailability(
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
    // TEST 11
    // =========================================================

    @Test
    void shouldThrowExceptionWhenDonorProfileNotFoundWhileUpdatingAvailability() {

        User user = new User();
        user.setId(9L);

        AvailabilityRequest request =
                new AvailabilityRequest();

        request.setAvailable(false);

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorProfileService.updateAvailability(
                        request,
                        9L
                )
        );

        assertEquals(
                "Donor profile not found",
                exception.getMessage()
        );
    }
}