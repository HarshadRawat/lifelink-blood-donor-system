package com.example.blooddonor.service;

import com.example.blooddonor.dto.AvailabilityRequest;
import com.example.blooddonor.dto.DonorProfileRequest;
import com.example.blooddonor.dto.DonorProfileResponse;
import com.example.blooddonor.entity.DonorProfile;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.DonorProfileRepository;
import com.example.blooddonor.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class DonorProfileService {

    private final DonorProfileRepository donorProfileRepository;
    private final UserRepository userRepository;

    public DonorProfileService(
            DonorProfileRepository donorProfileRepository,
            UserRepository userRepository) {

        this.donorProfileRepository = donorProfileRepository;
        this.userRepository = userRepository;
    }

    public DonorProfileResponse createProfile(
            DonorProfileRequest request,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        DonorProfile profile = new DonorProfile();

        profile.setBloodGroup(request.getBloodGroup());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        profile.setContactNumber(request.getContactNumber());
        profile.setAvailable(request.isAvailable());

        profile.setUser(user);

        DonorProfile savedProfile =
                donorProfileRepository.save(profile);

        DonorProfileResponse response =
                new DonorProfileResponse();

        response.setId(savedProfile.getId());
        response.setUserId(savedProfile.getUser().getId());
        response.setBloodGroup(savedProfile.getBloodGroup());
        response.setAddress(savedProfile.getAddress());
        response.setCity(savedProfile.getCity());
        response.setLatitude(savedProfile.getLatitude());
        response.setLongitude(savedProfile.getLongitude());
        response.setContactNumber(savedProfile.getContactNumber());
        response.setAvailable(savedProfile.isAvailable());

        return response;
    }

    public DonorProfileResponse updateProfile(
            DonorProfileRequest request,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        DonorProfile profile = donorProfileRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Donor profile not found"));

        profile.setBloodGroup(request.getBloodGroup());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        profile.setContactNumber(request.getContactNumber());
        profile.setAvailable(request.isAvailable());

        DonorProfile updatedProfile =
                donorProfileRepository.save(profile);

        DonorProfileResponse response =
                new DonorProfileResponse();

        response.setId(updatedProfile.getId());
        response.setUserId(updatedProfile.getUser().getId());
        response.setBloodGroup(updatedProfile.getBloodGroup());
        response.setAddress(updatedProfile.getAddress());
        response.setCity(updatedProfile.getCity());
        response.setLatitude(updatedProfile.getLatitude());
        response.setLongitude(updatedProfile.getLongitude());
        response.setContactNumber(updatedProfile.getContactNumber());
        response.setAvailable(updatedProfile.isAvailable());

        return response;
    }

    public DonorProfileResponse getProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        DonorProfile profile = donorProfileRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Donor profile not found"));

        DonorProfileResponse response =
                new DonorProfileResponse();

        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setBloodGroup(profile.getBloodGroup());
        response.setAddress(profile.getAddress());
        response.setCity(profile.getCity());
        response.setLatitude(profile.getLatitude());
        response.setLongitude(profile.getLongitude());
        response.setContactNumber(profile.getContactNumber());
        response.setAvailable(profile.isAvailable());

        return response;
    }

    public DonorProfileResponse updateAvailability(
            AvailabilityRequest request,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        DonorProfile profile = donorProfileRepository.findByUser(user)
                .orElseThrow(() ->
                        new RuntimeException("Donor profile not found"));

        profile.setAvailable(request.getAvailable());

        DonorProfile updatedProfile =
                donorProfileRepository.save(profile);

        DonorProfileResponse response =
                new DonorProfileResponse();

        response.setId(updatedProfile.getId());
        response.setUserId(updatedProfile.getUser().getId());
        response.setBloodGroup(updatedProfile.getBloodGroup());
        response.setAddress(updatedProfile.getAddress());
        response.setCity(updatedProfile.getCity());
        response.setLatitude(updatedProfile.getLatitude());
        response.setLongitude(updatedProfile.getLongitude());
        response.setContactNumber(updatedProfile.getContactNumber());
        response.setAvailable(updatedProfile.isAvailable());

        return response;
    }
}