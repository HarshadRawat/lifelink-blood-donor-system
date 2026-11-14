package com.example.blooddonor.config;

import com.example.blooddonor.entity.*;
import com.example.blooddonor.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Seeds realistic demonstration data (Hospital, 5 Donors across Bangalore cluster,
 * active emergency request, and real-time notifications) when activated under the
 * "demo" profile or via {@code app.demo.seed=true} / {@code DEMO_SEED_ENABLED=true}.
 *
 * Designed to let recruiters, interviewers, and evaluators test the platform end-to-end
 * within 30 seconds without manual setup.
 */
@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final UserRepository userRepository;
    private final HospitalProfileRepository hospitalProfileRepository;
    private final DonorProfileRepository donorProfileRepository;
    private final EmergencyRequestRepository emergencyRequestRepository;
    private final RequestDonorMatchRepository requestDonorMatchRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${app.demo.seed:false}")
    private boolean demoSeedEnabled;

    public DemoDataInitializer(
            UserRepository userRepository,
            HospitalProfileRepository hospitalProfileRepository,
            DonorProfileRepository donorProfileRepository,
            EmergencyRequestRepository emergencyRequestRepository,
            RequestDonorMatchRepository requestDonorMatchRepository,
            NotificationRepository notificationRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        this.userRepository = userRepository;
        this.hospitalProfileRepository = hospitalProfileRepository;
        this.donorProfileRepository = donorProfileRepository;
        this.emergencyRequestRepository = emergencyRequestRepository;
        this.requestDonorMatchRepository = requestDonorMatchRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean isDemoProfile = Arrays.asList(environment.getActiveProfiles()).contains("demo");
        if (!demoSeedEnabled && !isDemoProfile) {
            log.debug("Demo data seeding is disabled (profile: {}, app.demo.seed: {})",
                    Arrays.toString(environment.getActiveProfiles()), demoSeedEnabled);
            return;
        }

        if (userRepository.findByEmail("hospital@lifelink.com").isPresent()) {
            log.info("[LifeLink Demo] Demo dataset already initialized. Skipping seed.");
            return;
        }

        log.info("[LifeLink Demo] Initializing realistic Bangalore demonstration dataset...");

        // ====================================================================
        // 1. Demo Hospital: City Care Hospital (Central Bengaluru: 12.9352, 77.6245)
        // ====================================================================
        User hospUser = new User();
        hospUser.setName("City Care Hospital");
        hospUser.setEmail("hospital@lifelink.com");
        hospUser.setPassword(passwordEncoder.encode("Hospital123!"));
        hospUser.setRole(Role.HOSPITAL);
        hospUser = userRepository.save(hospUser);

        HospitalProfile hospProfile = new HospitalProfile();
        hospProfile.setUser(hospUser);
        hospProfile.setHospitalName("City Care Hospital");
        hospProfile.setRegistrationNumber("KA-BLR-HOSP-2026-001");
        hospProfile.setAddress("100 Hospital Boulevard, Koramangala");
        hospProfile.setCity("Bengaluru");
        hospProfile.setLatitude(12.9352);
        hospProfile.setLongitude(77.6245);
        hospProfile.setContactNumber("+91-8012345678");
        hospProfile = hospitalProfileRepository.save(hospProfile);

        // ====================================================================
        // 2. Demo Donors: Geographic cluster around Koramangala & Bangalore
        // ====================================================================

        // Donor 1: Alice Sharma (A+, ~0.5 km away -> DIRECT COMPATIBLE & NEARBY)
        User aliceUser = createDemoUser("Alice Sharma", "alice.donor@lifelink.com", "Donor123!", Role.DONOR);
        DonorProfile aliceProfile = createDemoDonor(aliceUser, "A+",
                "Flat 402, Green Glen Layout, Bellandur", "Bengaluru", 12.9380, 77.6290, "+91-9876543210", true);

        // Donor 2: Bob Rao (O-, ~0.8 km away -> UNIVERSAL DONOR & NEARBY)
        User bobUser = createDemoUser("Bob Rao", "bob.donor@lifelink.com", "Donor123!", Role.DONOR);
        DonorProfile bobProfile = createDemoDonor(bobUser, "O-",
                "22 1st Cross, Koramangala 4th Block", "Bengaluru", 12.9400, 77.6200, "+91-9876543211", true);

        // Donor 3: Charlie Kumar (B+, ~4.5 km away -> INCOMPATIBLE FOR A+ REQUEST)
        User charlieUser = createDemoUser("Charlie Kumar", "charlie.donor@lifelink.com", "Donor123!", Role.DONOR);
        createDemoDonor(charlieUser, "B+",
                "15 10th Main, Jayanagar", "Bengaluru", 12.9250, 77.5850, "+91-9876543212", true);

        // Donor 4: David Fernandes (AB+, ~6.5 km away -> UNIVERSAL RECIPIENT)
        User davidUser = createDemoUser("David Fernandes", "david.donor@lifelink.com", "Donor123!", Role.DONOR);
        createDemoDonor(davidUser, "AB+",
                "77 100ft Road, Indiranagar", "Bengaluru", 12.9780, 77.6400, "+91-9876543213", true);

        // Donor 5: Emma Watson (A+, ~60 km away in Tumkur -> OUTSIDE 20 KM RADIUS)
        User emmaUser = createDemoUser("Emma Watson", "emma.donor@lifelink.com", "Donor123!", Role.DONOR);
        createDemoDonor(emmaUser, "A+",
                "5 Bypass Road", "Tumkur", 13.3400, 77.1000, "+91-9876543214", true);

        // ====================================================================
        // 3. Demo Emergency Request & Initial Donor Matches
        // ====================================================================
        EmergencyRequest bloodReq = new EmergencyRequest();
        bloodReq.setHospital(hospProfile);
        bloodReq.setBloodGroup("A+");
        bloodReq.setUnitsRequired(2);
        bloodReq.setUrgency(Urgency.CRITICAL);
        bloodReq.setStatus(RequestStatus.ACTIVE);
        bloodReq.setCreatedAt(LocalDateTime.now().minusMinutes(15));
        bloodReq = emergencyRequestRepository.save(bloodReq);

        // Match 1: Alice matched (pending response)
        RequestDonorMatch aliceMatch = new RequestDonorMatch();
        aliceMatch.setEmergencyRequest(bloodReq);
        aliceMatch.setDonor(aliceProfile);
        aliceMatch.setStatus(MatchStatus.MATCHED);
        aliceMatch.setCreatedAt(LocalDateTime.now().minusMinutes(12));
        aliceMatch.setContactSharingConsent(false);
        requestDonorMatchRepository.save(aliceMatch);

        // Match 2: Bob accepted with contact sharing consent (ready for contact reveal demonstration)
        RequestDonorMatch bobMatch = new RequestDonorMatch();
        bobMatch.setEmergencyRequest(bloodReq);
        bobMatch.setDonor(bobProfile);
        bobMatch.setStatus(MatchStatus.ACCEPTED);
        bobMatch.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        bobMatch.setContactSharingConsent(true);
        requestDonorMatchRepository.save(bobMatch);

        // ====================================================================
        // 4. Sample Notifications
        // ====================================================================
        createNotification(aliceUser, bloodReq, NotificationType.MATCH_FOUND,
                "Urgent Blood Match Found",
                "City Care Hospital in Koramangala urgently needs 2 units of A+ blood. You are ~0.5 km away.");

        createNotification(hospUser, bloodReq, NotificationType.MATCH_ACCEPTED,
                "Donor Accepted Emergency Request",
                "Bob Rao (O-) accepted emergency blood request #" + bloodReq.getId() + " with contact sharing consent.");

        log.info("[LifeLink Demo] Successfully seeded demo cluster: 1 Hospital, 5 Donors, 1 Active Request with 2 matches.");
    }

    private User createDemoUser(String name, String email, String password, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }

    private DonorProfile createDemoDonor(User user, String bloodGroup, String address, String city,
                                         double lat, double lng, String phone, boolean available) {
        DonorProfile profile = new DonorProfile();
        profile.setUser(user);
        profile.setBloodGroup(bloodGroup);
        profile.setAddress(address);
        profile.setCity(city);
        profile.setLatitude(lat);
        profile.setLongitude(lng);
        profile.setContactNumber(phone);
        profile.setAvailable(available);
        return donorProfileRepository.save(profile);
    }

    private void createNotification(User user, EmergencyRequest request, NotificationType type,
                                    String title, String message) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setEmergencyRequest(request);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now().minusMinutes(8));
        notificationRepository.save(notification);
    }
}
