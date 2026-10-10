package com.example.blooddonor.repository;

import com.example.blooddonor.entity.DonorProfile;
import com.example.blooddonor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonorProfileRepository
        extends JpaRepository<DonorProfile, Long> {

    Optional<DonorProfile> findByUser(User user);

    List<DonorProfile> findByBloodGroupInAndAvailable(
            List<String> bloodGroups,
            boolean available
    );
}