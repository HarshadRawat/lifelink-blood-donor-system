package com.example.blooddonor.repository.specification;

import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Urgency;
import org.springframework.data.jpa.domain.Specification;

public class EmergencyRequestSpecification {

    public static Specification<EmergencyRequest> hasHospital(
            HospitalProfile hospital) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("hospital"),
                        hospital
                );
    }

    public static Specification<EmergencyRequest> hasStatus(
            RequestStatus status) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("status"),
                        status
                );
    }

    public static Specification<EmergencyRequest> hasBloodGroup(
            String bloodGroup) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("bloodGroup"),
                        bloodGroup
                );
    }

    public static Specification<EmergencyRequest> hasUrgency(
            Urgency urgency) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("urgency"),
                        urgency
                );
    }
}