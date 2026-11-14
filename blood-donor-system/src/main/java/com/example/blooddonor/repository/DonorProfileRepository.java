package com.example.blooddonor.repository;

import com.example.blooddonor.entity.DonorProfile;
import com.example.blooddonor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DonorProfileRepository
        extends JpaRepository<DonorProfile, Long> {

    Optional<DonorProfile> findByUser(User user);

    List<DonorProfile> findByBloodGroupInAndAvailable(
            List<String> bloodGroups,
            boolean available
    );

    @Query("""
        SELECT d FROM DonorProfile d
        WHERE d.available = true
          AND d.bloodGroup IN :groups
          AND d.latitude BETWEEN :minLat AND :maxLat
          AND d.longitude BETWEEN :minLng AND :maxLng
    """)
    List<DonorProfile> findCandidates(
            @Param("groups") List<String> groups,
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng
    );
}