package com.example.blooddonor.service;

import com.example.blooddonor.dto.DonorContactResponse;
import com.example.blooddonor.dto.MatchStatusRequest;
import com.example.blooddonor.dto.RequestDonorMatchResponse;
import com.example.blooddonor.entity.DonorProfile;
import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.MatchStatus;
import com.example.blooddonor.entity.RequestDonorMatch;
import com.example.blooddonor.entity.RequestStatus;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.exception.BusinessRuleException;
import com.example.blooddonor.repository.DonorProfileRepository;
import com.example.blooddonor.repository.EmergencyRequestRepository;
import com.example.blooddonor.repository.RequestDonorMatchRepository;
import com.example.blooddonor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DonorMatchingServiceTest {

    @Mock
    private DonorProfileRepository donorProfileRepository;

    @Mock
    private EmergencyRequestRepository emergencyRequestRepository;

    @Mock
    private RequestDonorMatchRepository requestDonorMatchRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DonorMatchingService donorMatchingService;

    @Mock
    private NotificationService notificationService;


    // =========================================================
    // TEST #47
    // Find nearby compatible donors and sort by distance
    // =========================================================

    @Test
    void shouldMatchNearbyCompatibleDonorsAndSortByDistance() {

        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(3L);
        hospital.setUser(hospitalUser);
        hospital.setLatitude(12.9698);
        hospital.setLongitude(77.7499);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setBloodGroup("O+");
        request.setUnitsRequired(1);
        request.setStatus(RequestStatus.ACTIVE);
        request.setHospital(hospital);

        User donorUser1 = new User();
        donorUser1.setId(9L);
        donorUser1.setName("Nearby Donor");

        DonorProfile donor1 = new DonorProfile();
        donor1.setId(1L);
        donor1.setUser(donorUser1);
        donor1.setBloodGroup("O+");
        donor1.setAvailable(true);
        donor1.setLatitude(12.9700);
        donor1.setLongitude(77.7500);

        User donorUser2 = new User();
        donorUser2.setId(10L);
        donorUser2.setName("Farther Donor");

        DonorProfile donor2 = new DonorProfile();
        donor2.setId(2L);
        donor2.setUser(donorUser2);
        donor2.setBloodGroup("O-");
        donor2.setAvailable(true);
        donor2.setLatitude(12.9750);
        donor2.setLongitude(77.7550);

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(donorProfileRepository
                .findByBloodGroupInAndAvailable(anyList(), any(Boolean.class)))
                .thenReturn(List.of(donor2, donor1));

        when(requestDonorMatchRepository
                .existsByEmergencyRequestAndDonor(request, donor1))
                .thenReturn(false);

        when(requestDonorMatchRepository
                .existsByEmergencyRequestAndDonor(request, donor2))
                .thenReturn(false);

        when(requestDonorMatchRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {

                    List<RequestDonorMatch> matches =
                            invocation.getArgument(0);

                    long id = 1L;

                    for (RequestDonorMatch match : matches) {
                        match.setId(id++);
                    }

                    return matches;
                });

        List<RequestDonorMatchResponse> responses =
                donorMatchingService.findMatches(1L);

        assertEquals(2, responses.size());

        // Nearest donor should come first
        assertEquals(1L, responses.get(0).getDonorId());
        assertEquals(2L, responses.get(1).getDonorId());

        assertEquals("Nearby Donor",
                responses.get(0).getDonorName());

        assertEquals("O+",
                responses.get(0).getBloodGroup());

        assertEquals(MatchStatus.MATCHED,
                responses.get(0).getStatus());
    }


    // =========================================================
    // TEST #48
    // Emergency request not found
    // =========================================================

    @Test
    void shouldThrowExceptionWhenEmergencyRequestNotFoundWhileFindingMatches() {

        when(emergencyRequestRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.findMatches(99L)
        );

        assertEquals(
                "Emergency request not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #49
    // Duplicate match should not be created
    // =========================================================

    @Test
    void shouldNotCreateDuplicateMatch() {

        HospitalProfile hospital = new HospitalProfile();
        hospital.setLatitude(12.9698);
        hospital.setLongitude(77.7499);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setBloodGroup("O+");
        request.setHospital(hospital);

        User donorUser = new User();
        donorUser.setId(9L);
        donorUser.setName("Test Donor");

        DonorProfile donor = new DonorProfile();
        donor.setId(1L);
        donor.setUser(donorUser);
        donor.setBloodGroup("O+");
        donor.setAvailable(true);
        donor.setLatitude(12.9698);
        donor.setLongitude(77.7499);

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(donorProfileRepository
                .findByBloodGroupInAndAvailable(anyList(), any(Boolean.class)))
                .thenReturn(List.of(donor));

        when(requestDonorMatchRepository
                .existsByEmergencyRequestAndDonor(request, donor))
                .thenReturn(true);

        when(requestDonorMatchRepository.saveAll(anyList()))
                .thenReturn(List.of());

        List<RequestDonorMatchResponse> responses =
                donorMatchingService.findMatches(1L);

        assertEquals(0, responses.size());
    }


    // =========================================================
    // TEST #50
    // Donor can accept own match
    // =========================================================

    @Test
    void shouldAllowDonorToAcceptOwnMatch() {

        User donorUser = new User();
        donorUser.setId(9L);
        donorUser.setName("Test Donor");

        DonorProfile donor = new DonorProfile();
        donor.setId(1L);
        donor.setUser(donorUser);
        donor.setBloodGroup("O+");

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setStatus(RequestStatus.ACTIVE);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(3L);
        hospital.setUser(new User());

        request.setHospital(hospital);

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setEmergencyRequest(request);
        match.setDonor(donor);
        match.setStatus(MatchStatus.MATCHED);
        match.setCreatedAt(LocalDateTime.now());

        MatchStatusRequest statusRequest =
                new MatchStatusRequest();

        statusRequest.setStatus(MatchStatus.ACCEPTED);

        when(requestDonorMatchRepository.findById(1L))
                .thenReturn(Optional.of(match));

        when(requestDonorMatchRepository.save(match))
                .thenReturn(match);

        RequestDonorMatchResponse response =
                donorMatchingService.updateMatchStatus(
                        1L,
                        statusRequest,
                        9L
                );

        assertEquals(1L, response.getId());
        assertEquals(1L, response.getDonorId());
        assertEquals(MatchStatus.ACCEPTED,
                response.getStatus());
    }


    // =========================================================
    // TEST #51
    // Unauthorized donor cannot update match
    // =========================================================

    @Test
    void shouldRejectUnauthorizedDonor() {

        User donorUser = new User();
        donorUser.setId(9L);

        DonorProfile donor = new DonorProfile();
        donor.setId(1L);
        donor.setUser(donorUser);

        EmergencyRequest request =
                new EmergencyRequest();

        request.setStatus(RequestStatus.ACTIVE);

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setDonor(donor);
        match.setEmergencyRequest(request);

        MatchStatusRequest statusRequest =
                new MatchStatusRequest();

        statusRequest.setStatus(MatchStatus.ACCEPTED);

        when(requestDonorMatchRepository.findById(1L))
                .thenReturn(Optional.of(match));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.updateMatchStatus(
                        1L,
                        statusRequest,
                        99L
                )
        );

        assertEquals(
                "You are not authorized to update this match",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #52
    // Match not found
    // =========================================================

    @Test
    void shouldThrowExceptionWhenMatchNotFound() {

        MatchStatusRequest request =
                new MatchStatusRequest();

        request.setStatus(MatchStatus.ACCEPTED);

        when(requestDonorMatchRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.updateMatchStatus(
                        99L,
                        request,
                        9L
                )
        );

        assertEquals(
                "Match not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #53
    // Cannot accept inactive emergency request
    // =========================================================

    @Test
    void shouldRejectAcceptingInactiveRequest() {

        User donorUser = new User();
        donorUser.setId(9L);

        DonorProfile donor = new DonorProfile();
        donor.setId(1L);
        donor.setUser(donorUser);

        EmergencyRequest emergencyRequest =
                new EmergencyRequest();

        emergencyRequest.setStatus(
                RequestStatus.FULFILLED
        );

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setDonor(donor);
        match.setEmergencyRequest(emergencyRequest);

        MatchStatusRequest request =
                new MatchStatusRequest();

        request.setStatus(MatchStatus.ACCEPTED);

        when(requestDonorMatchRepository.findById(1L))
                .thenReturn(Optional.of(match));

        BusinessRuleException exception =
                assertThrows(
                        BusinessRuleException.class,
                        () -> donorMatchingService.updateMatchStatus(
                                1L,
                                request,
                                9L
                        )
                );

        assertEquals(
                "This emergency request is no longer active",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #54
    // Invalid match status
    // =========================================================

    @Test
    void shouldRejectInvalidMatchStatus() {

        User donorUser = new User();
        donorUser.setId(9L);

        DonorProfile donor = new DonorProfile();
        donor.setId(1L);
        donor.setUser(donorUser);

        EmergencyRequest emergencyRequest =
                new EmergencyRequest();

        emergencyRequest.setStatus(
                RequestStatus.ACTIVE
        );

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setDonor(donor);
        match.setEmergencyRequest(emergencyRequest);

        MatchStatusRequest request =
                new MatchStatusRequest();

        request.setStatus(MatchStatus.MATCHED);

        when(requestDonorMatchRepository.findById(1L))
                .thenReturn(Optional.of(match));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.updateMatchStatus(
                        1L,
                        request,
                        9L
                )
        );

        assertEquals(
                "Invalid match status",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #55
    // Complete accepted match and fulfill request
    // =========================================================

    @Test
    void shouldCompleteMatchAndFulfillRequest() {

        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital =
                new HospitalProfile();

        hospital.setId(3L);
        hospital.setUser(hospitalUser);

        User donorUser = new User();
        donorUser.setId(9L);
        donorUser.setName("Test Donor");

        DonorProfile donor =
                new DonorProfile();

        donor.setId(1L);
        donor.setUser(donorUser);
        donor.setBloodGroup("O+");

        EmergencyRequest request =
                new EmergencyRequest();

        request.setId(1L);
        request.setUnitsRequired(1);
        request.setStatus(RequestStatus.ACTIVE);
        request.setHospital(hospital);

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setEmergencyRequest(request);
        match.setDonor(donor);
        match.setStatus(MatchStatus.ACCEPTED);
        match.setCreatedAt(LocalDateTime.now());

        when(requestDonorMatchRepository.findById(1L))
                .thenReturn(Optional.of(match));

        when(requestDonorMatchRepository.save(match))
                .thenReturn(match);

        when(requestDonorMatchRepository
                .countByEmergencyRequestAndStatus(
                        request,
                        MatchStatus.COMPLETED
                ))
                .thenReturn(1L);

        when(requestDonorMatchRepository
                .findByEmergencyRequest(request))
                .thenReturn(List.of(match));

        when(requestDonorMatchRepository.saveAll(anyList()))
                .thenReturn(List.of(match));

        when(emergencyRequestRepository.save(request))
                .thenReturn(request);

        RequestDonorMatchResponse response =
                donorMatchingService.completeMatch(
                        1L,
                        8L
                );

        assertEquals(
                MatchStatus.COMPLETED,
                response.getStatus()
        );

        assertEquals(
                RequestStatus.FULFILLED,
                request.getStatus()
        );
    }


    // =========================================================
    // TEST #56
    // Match not found while completing
    // =========================================================

    @Test
    void shouldThrowExceptionWhenMatchNotFoundWhileCompleting() {

        when(requestDonorMatchRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.completeMatch(
                        99L,
                        8L
                )
        );

        assertEquals(
                "Match not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #57
    // Hospital not authorized to complete
    // =========================================================

    @Test
    void shouldRejectUnauthorizedHospitalWhileCompleting() {

        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital =
                new HospitalProfile();

        hospital.setUser(hospitalUser);

        EmergencyRequest request =
                new EmergencyRequest();

        request.setHospital(hospital);

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setEmergencyRequest(request);

        when(requestDonorMatchRepository.findById(1L))
                .thenReturn(Optional.of(match));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.completeMatch(
                        1L,
                        99L
                )
        );

        assertEquals(
                "You are not authorized to complete this match",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #58
    // Match must be accepted before completion
    // =========================================================

    @Test
    void shouldRejectCompletingNonAcceptedMatch() {

        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital =
                new HospitalProfile();

        hospital.setUser(hospitalUser);

        EmergencyRequest request =
                new EmergencyRequest();

        request.setHospital(hospital);

        User donorUser = new User();
        donorUser.setId(9L);

        DonorProfile donor =
                new DonorProfile();

        donor.setUser(donorUser);

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setEmergencyRequest(request);
        match.setDonor(donor);
        match.setStatus(MatchStatus.MATCHED);

        when(requestDonorMatchRepository.findById(1L))
                .thenReturn(Optional.of(match));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.completeMatch(
                        1L,
                        8L
                )
        );

        assertEquals(
                "Only an accepted match can be completed",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #59
    // Get donor's matches successfully
    // =========================================================

    @Test
    void shouldGetMyMatchesSuccessfully() {

        User user = new User();
        user.setId(9L);

        DonorProfile donor =
                new DonorProfile();

        donor.setId(1L);
        donor.setUser(user);
        donor.setBloodGroup("O+");

        EmergencyRequest request =
                new EmergencyRequest();

        request.setId(1L);

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setEmergencyRequest(request);
        match.setDonor(donor);
        match.setStatus(MatchStatus.MATCHED);
        match.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUser(user))
                .thenReturn(Optional.of(donor));

        when(requestDonorMatchRepository.findByDonor(donor))
                .thenReturn(List.of(match));

        List<RequestDonorMatchResponse> responses =
                donorMatchingService.getMyMatches(9L);

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(1L, responses.get(0).getDonorId());
        assertEquals("O+",
                responses.get(0).getBloodGroup());
        assertEquals(
                MatchStatus.MATCHED,
                responses.get(0).getStatus()
        );
    }


    // =========================================================
    // TEST #60
    // User not found while getting donor matches
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUserNotFoundWhileGettingMyMatches() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.getMyMatches(99L)
        );

        assertEquals(
                "User not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #61
    // Donor profile not found while getting matches
    // =========================================================

    @Test
    void shouldThrowExceptionWhenDonorProfileNotFoundWhileGettingMyMatches() {

        User user = new User();
        user.setId(9L);

        when(userRepository.findById(9L))
                .thenReturn(Optional.of(user));

        when(donorProfileRepository.findByUser(user))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.getMyMatches(9L)
        );

        assertEquals(
                "Donor profile not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #62
    // Hospital can get request matches
    // =========================================================

    @Test
    void shouldGetRequestMatchesSuccessfully() {

        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital =
                new HospitalProfile();

        hospital.setId(3L);
        hospital.setUser(hospitalUser);

        EmergencyRequest request =
                new EmergencyRequest();

        request.setId(1L);
        request.setHospital(hospital);

        User donorUser = new User();
        donorUser.setId(9L);
        donorUser.setName("Test Donor");

        DonorProfile donor =
                new DonorProfile();

        donor.setId(1L);
        donor.setUser(donorUser);
        donor.setBloodGroup("O+");

        RequestDonorMatch match =
                new RequestDonorMatch();

        match.setId(1L);
        match.setEmergencyRequest(request);
        match.setDonor(donor);
        match.setStatus(MatchStatus.MATCHED);
        match.setCreatedAt(LocalDateTime.now());

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        when(requestDonorMatchRepository
                .findByEmergencyRequest(request))
                .thenReturn(List.of(match));

        List<RequestDonorMatchResponse> responses =
                donorMatchingService.getRequestMatches(
                        1L,
                        8L
                );

        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(1L, responses.get(0).getEmergencyRequestId());
        assertEquals(1L, responses.get(0).getDonorId());
        assertEquals(
                "Test Donor",
                responses.get(0).getDonorName()
        );
        assertEquals(
                "O+",
                responses.get(0).getBloodGroup()
        );
        assertEquals(
                MatchStatus.MATCHED,
                responses.get(0).getStatus()
        );
    }


    // =========================================================
    // TEST #63
    // Emergency request not found while getting matches
    // =========================================================

    @Test
    void shouldThrowExceptionWhenEmergencyRequestNotFoundWhileGettingRequestMatches() {

        when(emergencyRequestRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.getRequestMatches(
                        99L,
                        8L
                )
        );

        assertEquals(
                "Emergency request not found",
                exception.getMessage()
        );
    }


    // =========================================================
    // TEST #64
    // Hospital cannot view another hospital's request
    // =========================================================

    @Test
    void shouldRejectUnauthorizedHospitalWhileGettingRequestMatches() {

        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital =
                new HospitalProfile();

        hospital.setUser(hospitalUser);

        EmergencyRequest request =
                new EmergencyRequest();

        request.setId(1L);
        request.setHospital(hospital);

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> donorMatchingService.getRequestMatches(
                        1L,
                        99L
                )
        );

        assertEquals(
                "You are not allowed to view this request",
                exception.getMessage()
        );
    }

    // =========================================================
    // TEST #53
    // Donor can accept with explicit consent
    // =========================================================

    @Test
    void shouldAllowDonorToAcceptMatchWithExplicitConsent() {
        User donorUser = new User();
        donorUser.setId(9L);
        donorUser.setName("Test Donor");

        DonorProfile donor = new DonorProfile();
        donor.setId(1L);
        donor.setUser(donorUser);
        donor.setBloodGroup("O+");
        donor.setContactNumber("+1-555-0199");

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setStatus(RequestStatus.ACTIVE);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setId(3L);
        hospital.setUser(new User());
        request.setHospital(hospital);

        RequestDonorMatch match = new RequestDonorMatch();
        match.setId(10L);
        match.setEmergencyRequest(request);
        match.setDonor(donor);
        match.setStatus(MatchStatus.MATCHED);
        match.setCreatedAt(LocalDateTime.now());

        MatchStatusRequest statusRequest = new MatchStatusRequest();
        statusRequest.setStatus(MatchStatus.ACCEPTED);
        statusRequest.setConsentToShareContact(true);

        when(requestDonorMatchRepository.findById(10L))
                .thenReturn(Optional.of(match));
        when(requestDonorMatchRepository.save(match))
                .thenReturn(match);

        RequestDonorMatchResponse response =
                donorMatchingService.updateMatchStatus(10L, statusRequest, 9L);

        assertEquals(MatchStatus.ACCEPTED, response.getStatus());
        assertTrue(response.getContactSharingConsent());
        assertTrue(match.getContactSharingConsent());
    }

    // =========================================================
    // TEST #54
    // getRequestMatches exposes contact details ONLY when accepted and consented
    // =========================================================

    @Test
    void shouldExposeContactDetailsInGetRequestMatchesOnlyWhenAcceptedAndConsented() {
        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setUser(hospitalUser);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setHospital(hospital);

        User donorUser = new User();
        donorUser.setId(9L);
        donorUser.setName("Jane Donor");
        donorUser.setEmail("jane@donor.org");

        DonorProfile donor = new DonorProfile();
        donor.setId(2L);
        donor.setUser(donorUser);
        donor.setBloodGroup("O-");
        donor.setContactNumber("+1-555-0144");

        // Match 1: ACCEPTED with consent
        RequestDonorMatch match1 = new RequestDonorMatch();
        match1.setId(101L);
        match1.setEmergencyRequest(request);
        match1.setDonor(donor);
        match1.setStatus(MatchStatus.ACCEPTED);
        match1.setContactSharingConsent(true);
        match1.setCreatedAt(LocalDateTime.now());

        // Match 2: MATCHED (pending, not accepted yet)
        RequestDonorMatch match2 = new RequestDonorMatch();
        match2.setId(102L);
        match2.setEmergencyRequest(request);
        match2.setDonor(donor);
        match2.setStatus(MatchStatus.MATCHED);
        match2.setContactSharingConsent(false);
        match2.setCreatedAt(LocalDateTime.now());

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));
        when(requestDonorMatchRepository.findByEmergencyRequest(request))
                .thenReturn(List.of(match1, match2));

        List<RequestDonorMatchResponse> responses =
                donorMatchingService.getRequestMatches(1L, 8L);

        assertEquals(2, responses.size());

        // Match 1 has contact info
        assertEquals("+1-555-0144", responses.get(0).getContactNumber());
        assertEquals("jane@donor.org", responses.get(0).getEmail());
        assertTrue(responses.get(0).getContactSharingConsent());

        // Match 2 has contact info HIDDEN (null)
        assertNull(responses.get(1).getContactNumber());
        assertNull(responses.get(1).getEmail());
    }

    // =========================================================
    // TEST #55
    // Hospital owner can retrieve donor contact details via getDonorContact
    // =========================================================

    @Test
    void shouldAllowHospitalOwnerToGetDonorContact() {
        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setUser(hospitalUser);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setHospital(hospital);

        User donorUser = new User();
        donorUser.setId(9L);
        donorUser.setName("Jane Donor");
        donorUser.setEmail("jane@donor.org");

        DonorProfile donor = new DonorProfile();
        donor.setId(2L);
        donor.setUser(donorUser);
        donor.setBloodGroup("O-");
        donor.setContactNumber("+1-555-0144");

        RequestDonorMatch match = new RequestDonorMatch();
        match.setId(101L);
        match.setEmergencyRequest(request);
        match.setDonor(donor);
        match.setStatus(MatchStatus.ACCEPTED);
        match.setContactSharingConsent(true);

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));
        when(requestDonorMatchRepository.findById(101L))
                .thenReturn(Optional.of(match));

        DonorContactResponse contact =
                donorMatchingService.getDonorContact(1L, 101L, 8L);

        assertEquals(101L, contact.getMatchId());
        assertEquals(1L, contact.getRequestId());
        assertEquals("Jane Donor", contact.getDonorName());
        assertEquals("O-", contact.getBloodGroup());
        assertEquals("+1-555-0144", contact.getContactNumber());
        assertEquals("jane@donor.org", contact.getEmail());
        assertEquals(MatchStatus.ACCEPTED, contact.getStatus());
        assertTrue(contact.getContactSharingConsent());
    }

    // =========================================================
    // TEST #56
    // Different hospital cannot retrieve donor contact
    // =========================================================

    @Test
    void shouldRejectUnauthorizedHospitalFromGettingDonorContact() {
        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setUser(hospitalUser);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setHospital(hospital);

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        SecurityException exception = assertThrows(
                SecurityException.class,
                () -> donorMatchingService.getDonorContact(1L, 101L, 999L)
        );

        assertEquals(
                "You are not authorized to view contact details for this request",
                exception.getMessage()
        );
    }

    // =========================================================
    // TEST #57
    // Cannot get contact details if match is not accepted
    // =========================================================

    @Test
    void shouldRejectGetDonorContactWhenMatchNotAccepted() {
        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setUser(hospitalUser);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setHospital(hospital);

        RequestDonorMatch match = new RequestDonorMatch();
        match.setId(101L);
        match.setEmergencyRequest(request);
        match.setStatus(MatchStatus.MATCHED);

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));
        when(requestDonorMatchRepository.findById(101L))
                .thenReturn(Optional.of(match));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> donorMatchingService.getDonorContact(1L, 101L, 8L)
        );

        assertEquals(
                "Contact details are only available for accepted donor matches",
                exception.getMessage()
        );
    }

    // =========================================================
    // TEST #58
    // Cannot get contact details if donor has not consented
    // =========================================================

    @Test
    void shouldRejectGetDonorContactWhenConsentNotGranted() {
        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setUser(hospitalUser);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setStatus(RequestStatus.ACTIVE);
        request.setHospital(hospital);

        RequestDonorMatch match = new RequestDonorMatch();
        match.setId(101L);
        match.setEmergencyRequest(request);
        match.setStatus(MatchStatus.ACCEPTED);
        match.setContactSharingConsent(false);

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));
        when(requestDonorMatchRepository.findById(101L))
                .thenReturn(Optional.of(match));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> donorMatchingService.getDonorContact(1L, 101L, 8L)
        );

        assertEquals(
                "Donor has not consented to share contact details",
                exception.getMessage()
        );
    }

    // =========================================================
    // TEST #59
    // Cannot get contact details if request is cancelled
    // =========================================================

    @Test
    void shouldRejectGetDonorContactWhenRequestIsCancelled() {
        User hospitalUser = new User();
        hospitalUser.setId(8L);

        HospitalProfile hospital = new HospitalProfile();
        hospital.setUser(hospitalUser);

        EmergencyRequest request = new EmergencyRequest();
        request.setId(1L);
        request.setStatus(RequestStatus.CANCELLED);
        request.setHospital(hospital);

        when(emergencyRequestRepository.findById(1L))
                .thenReturn(Optional.of(request));

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> donorMatchingService.getDonorContact(1L, 101L, 8L)
        );

        assertEquals(
                "Cannot view contact details for a cancelled emergency request",
                exception.getMessage()
        );
    }
}
