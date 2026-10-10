package com.example.blooddonor.repository;

import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HospitalProfileRepository
        extends JpaRepository<HospitalProfile, Long> {

    Optional<HospitalProfile> findByUser(User user);
}