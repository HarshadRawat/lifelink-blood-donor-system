package com.example.blooddonor.integration;

import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Urgency;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
class EmergencyRequestConcurrencyTest {

    @Autowired
    private EmergencyRequestRepository emergencyRequestRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Optimistic Locking: Concurrent updates to same EmergencyRequest must throw ObjectOptimisticLockingFailureException")
    void concurrentUpdate_shouldThrowOptimisticLockingFailureException() {
        // 1. Arrange: Create initial EmergencyRequest
        EmergencyRequest initial = new EmergencyRequest();
        initial.setBloodGroup("O+");
        initial.setUnitsRequired(4);
        initial.setUrgency(Urgency.CRITICAL);
        initial.setStatus(RequestStatus.ACTIVE);
        initial.setCreatedAt(LocalDateTime.now());

        EmergencyRequest saved = emergencyRequestRepository.saveAndFlush(initial);
        Long requestId = saved.getId();
        entityManager.clear();

        // 2. Simulate User 1 loading the request
        EmergencyRequest user1View = emergencyRequestRepository.findById(requestId).orElseThrow();

        // 3. Simulate User 2 loading the request concurrently
        entityManager.detach(user1View);
        EmergencyRequest user2View = emergencyRequestRepository.findById(requestId).orElseThrow();

        // 4. User 1 updates units required and saves successfully (version increments from 0 to 1)
        user1View.setUnitsRequired(2);
        emergencyRequestRepository.saveAndFlush(user1View);
        entityManager.clear();

        // 5. User 2 attempts to save with stale version 0 -> throws ObjectOptimisticLockingFailureException
        user2View.setStatus(RequestStatus.FULFILLED);
        assertThrows(ObjectOptimisticLockingFailureException.class, () -> {
            emergencyRequestRepository.saveAndFlush(user2View);
        });
    }
}
