package com.example.blooddonor.service;

import com.example.blooddonor.dto.EmergencyRequestRequest;
import com.example.blooddonor.dto.EmergencyRequestResponse;
import com.example.blooddonor.dto.EmergencyRequestStatusRequest;
import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.Urgency;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.HospitalProfileRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import com.example.blooddonor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmergencyRequestServiceTest {

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private HospitalProfileRepository hospitalProfileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestDonorMatchRepository requestDonorMatchRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EmergencyRequestService emergencyRequestService;


    @Test
    void shouldCreateEmergencyRequest() {

        // Hospital user
        User hospitalUser = new User();
        hospitalUser.setId(1L);
        hospitalUser.setName("Test Hospital");


        // Hospital profile
        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setUser(hospitalUser);


        // Request DTO
        EmergencyRequestRequest request =
                new EmergencyRequestRequest();

        request.setBloodGroup("O+");
        request.setUnitsRequired(2);
        request.setUrgency(Urgency.HIGH);


        // Mock user lookup
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));


        // Mock hospital lookup
        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));


        // Mock save
        EmergencyRequest savedRequest =
                new EmergencyRequest();

        savedRequest.setId(1L);
        savedRequest.setBloodGroup("O+");
        savedRequest.setUnitsRequired(2);
        savedRequest.setUrgency(Urgency.HIGH);
        savedRequest.setStatus(RequestStatus.ACTIVE);
        savedRequest.setHospital(hospital);


        when(emergencyRequestRepository.save(
                any(EmergencyRequest.class)))
                .thenReturn(savedRequest);


        // Call service
        EmergencyRequestResponse result =
                emergencyRequestService.createRequest(
                        request,
                        1L
                );


        // Verify response
        assertEquals(1L, result.getId());
        assertEquals("O+", result.getBloodGroup());
        assertEquals(2, result.getUnitsRequired());
        assertEquals(Urgency.HIGH, result.getUrgency());
        assertEquals(RequestStatus.ACTIVE, result.getStatus());


        // Verify database save
        verify(emergencyRequestRepository)
                .save(any(EmergencyRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenHospitalProfileNotFound() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        EmergencyRequestRequest request =
                new EmergencyRequestRequest();

        request.setBloodGroup("O+");
        request.setUnitsRequired(2);
        request.setUrgency(Urgency.HIGH);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> emergencyRequestService.createRequest(
                        request,
                        1L
                )
        );

        assertEquals(
                "Hospital profile not found",
                exception.getMessage()
        );

        verify(emergencyRequestRepository, never())
                .save(any(EmergencyRequest.class));
    }

    @Test
    void shouldUpdateEmergencyRequestStatus() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setUser(hospitalUser);

        EmergencyRequest existingRequest =
                new EmergencyRequest();

        existingRequest.setId(1L);
        existingRequest.setBloodGroup("O+");
        existingRequest.setUnitsRequired(2);
        existingRequest.setUrgency(Urgency.HIGH);
        existingRequest.setStatus(RequestStatus.ACTIVE);
        existingRequest.setHospital(hospital);

        EmergencyRequestStatusRequest statusRequest =
                new EmergencyRequestStatusRequest();

        statusRequest.setStatus(RequestStatus.CANCELLED);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(existingRequest));

        when(emergencyRequestRepository.save(existingRequest))
                .thenReturn(existingRequest);

        EmergencyRequestResponse result =
                emergencyRequestService.updateStatus(
                        1L,
                        statusRequest,
                        1L
                );

        assertEquals(
                RequestStatus.CANCELLED,
                result.getStatus()
        );

        assertEquals(
                RequestStatus.CANCELLED,
                existingRequest.getStatus()
        );

        verify(emergencyRequestRepository)
                .save(existingRequest);
    }

    @Test
    void shouldRejectUnauthorizedHospitalUpdatingRequest() {

        // Logged-in hospital user
        User loggedInHospitalUser = new User();
        loggedInHospitalUser.setId(1L);

        HospitalProfile loggedInHospital = new HospitalProfile();
        loggedInHospital.setId(1L);
        loggedInHospital.setUser(loggedInHospitalUser);


        // Another hospital who actually owns the request
        User requestOwnerUser = new User();
        requestOwnerUser.setId(2L);

        HospitalProfile requestOwnerHospital = new HospitalProfile();
        requestOwnerHospital.setId(2L);
        requestOwnerHospital.setUser(requestOwnerUser);


        // Emergency request belongs to Hospital 2
        EmergencyRequest existingRequest =
                new EmergencyRequest();

        existingRequest.setId(1L);
        existingRequest.setBloodGroup("O+");
        existingRequest.setUnitsRequired(2);
        existingRequest.setUrgency(Urgency.HIGH);
        existingRequest.setStatus(RequestStatus.ACTIVE);
        existingRequest.setHospital(requestOwnerHospital);


        // Request to cancel the emergency request
        EmergencyRequestStatusRequest statusRequest =
                new EmergencyRequestStatusRequest();

        statusRequest.setStatus(RequestStatus.CANCELLED);


        // Logged-in user is Hospital 1
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(loggedInHospitalUser));

        when(hospitalProfileRepository.findByUser(loggedInHospitalUser))
                .thenReturn(Optional.of(loggedInHospital));


        // But request belongs to Hospital 2
        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(existingRequest));


        // Expect authorization exception
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> emergencyRequestService.updateStatus(
                        1L,
                        statusRequest,
                        1L
                )
        );


        // Request must remain unchanged
        assertEquals(
                RequestStatus.ACTIVE,
                existingRequest.getStatus()
        );


        // Database save must NOT happen
        verify(emergencyRequestRepository, never())
                .save(any(EmergencyRequest.class));
    }

    @Test
    void shouldThrowExceptionWhenEmergencyRequestNotFound() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setUser(hospitalUser);

        EmergencyRequestStatusRequest statusRequest =
                new EmergencyRequestStatusRequest();

        statusRequest.setStatus(RequestStatus.CANCELLED);


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));


        // Request does not exist
        when(emergencyRequestRepository.findById(999L))
                .thenReturn(Optional.empty());


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> emergencyRequestService.updateStatus(
                        999L,
                        statusRequest,
                        1L
                )
        );


        assertEquals(
                "Emergency request not found",
                exception.getMessage()
        );


        verify(emergencyRequestRepository, never())
                .save(any(EmergencyRequest.class));
    }

    @Test
    void shouldRejectUpdatingNonActiveRequest() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setUser(hospitalUser);


        // Existing request is already fulfilled
        EmergencyRequest existingRequest =
                new EmergencyRequest();

        existingRequest.setId(1L);
        existingRequest.setBloodGroup("O+");
        existingRequest.setUnitsRequired(2);
        existingRequest.setUrgency(Urgency.HIGH);
        existingRequest.setStatus(RequestStatus.FULFILLED);
        existingRequest.setHospital(hospital);


        EmergencyRequestStatusRequest statusRequest =
                new EmergencyRequestStatusRequest();

        statusRequest.setStatus(RequestStatus.CANCELLED);


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(existingRequest));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> emergencyRequestService.updateStatus(
                        1L,
                        statusRequest,
                        1L
                )
        );


        assertEquals(
                "Only an active request can be cancelled",
                exception.getMessage()
        );


        // Request must remain fulfilled
        assertEquals(
                RequestStatus.FULFILLED,
                existingRequest.getStatus()
        );


        // Nothing should be saved
        verify(emergencyRequestRepository, never())
                .save(any(EmergencyRequest.class));
    }

    @Test
    void shouldRejectInvalidStatusUpdate() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setUser(hospitalUser);


        EmergencyRequest existingRequest =
                new EmergencyRequest();

        existingRequest.setId(1L);
        existingRequest.setBloodGroup("O+");
        existingRequest.setUnitsRequired(2);
        existingRequest.setUrgency(Urgency.HIGH);
        existingRequest.setStatus(RequestStatus.ACTIVE);
        existingRequest.setHospital(hospital);


        // Try to change ACTIVE request to FULFILLED
        EmergencyRequestStatusRequest statusRequest =
                new EmergencyRequestStatusRequest();

        statusRequest.setStatus(RequestStatus.FULFILLED);


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(existingRequest));


        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> emergencyRequestService.updateStatus(
                        1L,
                        statusRequest,
                        1L
                )
        );


        assertEquals(
                "Hospital can only change an active request to CANCELLED",
                exception.getMessage()
        );


        // Request must remain ACTIVE
        assertEquals(
                RequestStatus.ACTIVE,
                existingRequest.getStatus()
        );


        // Nothing should be saved
        verify(emergencyRequestRepository, never())
                .save(any(EmergencyRequest.class));
    }

    @Test
    void shouldGetMyEmergencyRequests() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);
        hospitalUser.setName("Test Hospital");

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setHospitalName("Test Hospital");
        hospital.setUser(hospitalUser);


        EmergencyRequest request1 =
                new EmergencyRequest();

        request1.setId(1L);
        request1.setBloodGroup("O+");
        request1.setUnitsRequired(2);
        request1.setUrgency(Urgency.HIGH);
        request1.setStatus(RequestStatus.ACTIVE);
        request1.setHospital(hospital);


        EmergencyRequest request2 =
                new EmergencyRequest();

        request2.setId(2L);
        request2.setBloodGroup("A+");
        request2.setUnitsRequired(3);
        request2.setUrgency(Urgency.MEDIUM);
        request2.setStatus(RequestStatus.CANCELLED);
        request2.setHospital(hospital);


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));


        Pageable pageable =
                PageRequest.of(0, 10);

        Page<EmergencyRequest> requestPage =
                new PageImpl<>(
                        java.util.List.of(request1, request2),
                        pageable,
                        2
                );


        when(emergencyRequestRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(requestPage);


        Page<EmergencyRequestResponse> result =
                emergencyRequestService.getMyRequests(
                        1L,
                        null,
                        null,
                        null,
                        pageable
                );


        assertEquals(2, result.getTotalElements());

        assertEquals(
                1L,
                result.getContent().get(0).getId()
        );

        assertEquals(
                "O+",
                result.getContent().get(0).getBloodGroup()
        );

        assertEquals(
                RequestStatus.ACTIVE,
                result.getContent().get(0).getStatus()
        );

        assertEquals(
                2L,
                result.getContent().get(1).getId()
        );

        assertEquals(
                "A+",
                result.getContent().get(1).getBloodGroup()
        );

        assertEquals(
                RequestStatus.CANCELLED,
                result.getContent().get(1).getStatus()
        );


        verify(emergencyRequestRepository)
                .findAll(
                        any(Specification.class),
                        eq(pageable)
                );
    }

    @Test
    void shouldFilterMyRequestsByStatus() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setHospitalName("Test Hospital");
        hospital.setUser(hospitalUser);


        EmergencyRequest activeRequest =
                new EmergencyRequest();

        activeRequest.setId(1L);
        activeRequest.setBloodGroup("O+");
        activeRequest.setUnitsRequired(2);
        activeRequest.setUrgency(Urgency.HIGH);
        activeRequest.setStatus(RequestStatus.ACTIVE);
        activeRequest.setHospital(hospital);


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));


        Pageable pageable =
                PageRequest.of(0, 10);

        Page<EmergencyRequest> requestPage =
                new PageImpl<>(
                        java.util.List.of(activeRequest),
                        pageable,
                        1
                );


        when(emergencyRequestRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(requestPage);


        Page<EmergencyRequestResponse> result =
                emergencyRequestService.getMyRequests(
                        1L,
                        RequestStatus.ACTIVE,
                        null,
                        null,
                        pageable
                );


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                RequestStatus.ACTIVE,
                result.getContent().get(0).getStatus()
        );

        assertEquals(
                "O+",
                result.getContent().get(0).getBloodGroup()
        );


        verify(emergencyRequestRepository)
                .findAll(
                        any(Specification.class),
                        eq(pageable)
                );
    }

    @Test
    void shouldFilterMyRequestsByBloodGroup() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setHospitalName("Test Hospital");
        hospital.setUser(hospitalUser);


        EmergencyRequest oPositiveRequest =
                new EmergencyRequest();

        oPositiveRequest.setId(1L);
        oPositiveRequest.setBloodGroup("O+");
        oPositiveRequest.setUnitsRequired(2);
        oPositiveRequest.setUrgency(Urgency.HIGH);
        oPositiveRequest.setStatus(RequestStatus.ACTIVE);
        oPositiveRequest.setHospital(hospital);


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));


        Pageable pageable =
                PageRequest.of(0, 10);

        Page<EmergencyRequest> requestPage =
                new PageImpl<>(
                        java.util.List.of(oPositiveRequest),
                        pageable,
                        1
                );


        when(emergencyRequestRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(requestPage);


        Page<EmergencyRequestResponse> result =
                emergencyRequestService.getMyRequests(
                        1L,
                        null,
                        "O+",
                        null,
                        pageable
                );


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                "O+",
                result.getContent().get(0).getBloodGroup()
        );

        assertEquals(
                RequestStatus.ACTIVE,
                result.getContent().get(0).getStatus()
        );


        verify(emergencyRequestRepository)
                .findAll(
                        any(Specification.class),
                        eq(pageable)
                );
    }

    @Test
    void shouldFilterMyRequestsByUrgency() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(1L);
        hospital.setHospitalName("Test Hospital");
        hospital.setUser(hospitalUser);


        EmergencyRequest highUrgencyRequest =
                new EmergencyRequest();

        highUrgencyRequest.setId(1L);
        highUrgencyRequest.setBloodGroup("O+");
        highUrgencyRequest.setUnitsRequired(2);
        highUrgencyRequest.setUrgency(Urgency.HIGH);
        highUrgencyRequest.setStatus(RequestStatus.ACTIVE);
        highUrgencyRequest.setHospital(hospital);


        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.of(hospital));


        Pageable pageable =
                PageRequest.of(0, 10);

        Page<EmergencyRequest> requestPage =
                new PageImpl<>(
                        java.util.List.of(highUrgencyRequest),
                        pageable,
                        1
                );


        when(emergencyRequestRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(requestPage);


        Page<EmergencyRequestResponse> result =
                emergencyRequestService.getMyRequests(
                        1L,
                        null,
                        null,
                        Urgency.HIGH,
                        pageable
                );


        assertEquals(
                1,
                result.getTotalElements()
        );

        assertEquals(
                Urgency.HIGH,
                result.getContent().get(0).getUrgency()
        );

        assertEquals(
                "O+",
                result.getContent().get(0).getBloodGroup()
        );


        verify(emergencyRequestRepository)
                .findAll(
                        any(Specification.class),
                        eq(pageable)
                );
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileGettingRequests() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        Pageable pageable =
                PageRequest.of(0, 10);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> emergencyRequestService.getMyRequests(
                        999L,
                        null,
                        null,
                        null,
                        pageable
                )
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(hospitalProfileRepository, never())
                .findByUser(any(User.class));

        verify(emergencyRequestRepository, never())
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }

    @Test
    void shouldThrowExceptionWhenHospitalProfileNotFoundWhileGettingRequests() {

        User hospitalUser = new User();
        hospitalUser.setId(1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(hospitalUser));

        when(hospitalProfileRepository.findByUser(hospitalUser))
                .thenReturn(Optional.empty());

        Pageable pageable =
                PageRequest.of(0, 10);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> emergencyRequestService.getMyRequests(
                        1L,
                        null,
                        null,
                        null,
                        pageable
                )
        );

        assertEquals(
                "Hospital profile not found",
                exception.getMessage()
        );

        verify(emergencyRequestRepository, never())
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }
}


