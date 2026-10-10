package com.example.blooddonor.service;

import com.example.blooddonor.dto.HospitalProfileRequest;
import com.example.blooddonor.dto.HospitalProfileResponse;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.HospitalProfileRepository;
import com.example.blooddonor.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class HospitalProfileService {

    private final HospitalProfileRepository hospitalProfileRepository;
    private final UserRepository userRepository;

    public HospitalProfileService(
            HospitalProfileRepository hospitalProfileRepository,
            UserRepository userRepository) {

        this.hospitalProfileRepository = hospitalProfileRepository;
        this.userRepository = userRepository;
    }

    public HospitalProfileResponse createProfile(
            HospitalProfileRequest request,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        if (hospitalProfileRepository.findByUser(user).isPresent()) {
            throw new RuntimeException("Hospital profile already exists");
        }

        HospitalProfile profile = new HospitalProfile();

        profile.setHospitalName(request.getHospitalName());
        profile.setRegistrationNumber(
                request.getRegistrationNumber()
        );
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        profile.setContactNumber(request.getContactNumber());

        profile.setUser(user);

        HospitalProfile savedProfile =
                hospitalProfileRepository.save(profile);

        HospitalProfileResponse response =
                new HospitalProfileResponse();

        response.setId(savedProfile.getId());
        response.setUserId(savedProfile.getUser().getId());
        response.setHospitalName(savedProfile.getHospitalName());
        response.setRegistrationNumber(
                savedProfile.getRegistrationNumber()
        );
        response.setAddress(savedProfile.getAddress());
        response.setCity(savedProfile.getCity());
        response.setLatitude(savedProfile.getLatitude());
        response.setLongitude(savedProfile.getLongitude());
        response.setContactNumber(savedProfile.getContactNumber());

        return response;
    }

    public HospitalProfileResponse getProfile(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        HospitalProfile profile =
                hospitalProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException("Hospital profile not found"));

        HospitalProfileResponse response =
                new HospitalProfileResponse();

        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setHospitalName(profile.getHospitalName());
        response.setRegistrationNumber(
                profile.getRegistrationNumber()
        );
        response.setAddress(profile.getAddress());
        response.setCity(profile.getCity());
        response.setLatitude(profile.getLatitude());
        response.setLongitude(profile.getLongitude());
        response.setContactNumber(profile.getContactNumber());

        return response;
    }

    public HospitalProfileResponse updateProfile(
            HospitalProfileRequest request,
            Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        HospitalProfile profile =
                hospitalProfileRepository.findByUser(user)
                        .orElseThrow(() ->
                                new RuntimeException("Hospital profile not found"));

        profile.setHospitalName(request.getHospitalName());
        profile.setRegistrationNumber(
                request.getRegistrationNumber()
        );
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        profile.setContactNumber(request.getContactNumber());

        HospitalProfile updatedProfile =
                hospitalProfileRepository.save(profile);

        HospitalProfileResponse response =
                new HospitalProfileResponse();

        response.setId(updatedProfile.getId());
        response.setUserId(updatedProfile.getUser().getId());
        response.setHospitalName(updatedProfile.getHospitalName());
        response.setRegistrationNumber(
                updatedProfile.getRegistrationNumber()
        );
        response.setAddress(updatedProfile.getAddress());
        response.setCity(updatedProfile.getCity());
        response.setLatitude(updatedProfile.getLatitude());
        response.setLongitude(updatedProfile.getLongitude());
        response.setContactNumber(updatedProfile.getContactNumber());

        return response;
    }
}