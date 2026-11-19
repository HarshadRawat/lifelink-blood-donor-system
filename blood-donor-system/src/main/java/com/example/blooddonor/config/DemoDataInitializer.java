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
        if (demoSeedEnabled && !isDemoProfile) {
            throw new IllegalStateException("Demo data seeding requested (app.demo.seed=true) but 'demo' Spring profile is not active.");
        }
        if (!isDemoProfile) {
            log.debug("Demo data seeding is disabled (demo profile is not active).");
            return;
        }

        if (userRepository.findByEmail("hospital@lifelink.com").isPresent()) {
            log.info("[LifeLink Demo] Demo dataset already initialized. Skipping seed.");
            return;
        }

        log.info("[LifeLink Demo] Initializing realistic Bangalore demonstration dataset...");

        // ====================================================================
        // 1. Demo Hospitals: Verified & Pending Verification
        // ====================================================================
        // Hospital 1: City Care Hospital (Central Bengaluru: 12.9352, 77.6245) - Verified
        HospitalProfile hospProfile = createDemoHospital("City Care Hospital", "hospital@lifelink.com", "Hospital123!",
                "KA-BLR-HOSP-2026-001", "100 Hospital Boulevard, Koramangala", "Bengaluru",
                12.9352, 77.6245, "+91-8012345678", true);
        User hospUser = hospProfile.getUser();

        // Hospital 2: Metro Trauma Center (Indiranagar: 12.9716, 77.6412) - Verified
        HospitalProfile metroProfile = createDemoHospital("Metro Trauma Center", "metro.hospital@lifelink.com", "Hospital123!",
                "KA-BLR-HOSP-2026-002", "45 100ft Road, Indiranagar", "Bengaluru",
                12.9716, 77.6412, "+91-8022334455", true);

        // Hospital 3: St. Jude Community Hospital (Whitefield: 12.9698, 77.7500) - Unverified (Pending Admin Approval)
        createDemoHospital("St. Jude Community Hospital", "pending.hospital@lifelink.com", "Hospital123!",
                "KA-BLR-HOSP-2026-003", "12 ITPL Main Road, Whitefield", "Bengaluru",
                12.9698, 77.7500, "+91-8033445566", false);

        // ====================================================================
        // 2. Demo Donors: Geographic cluster covering ABO/Rh & Availability
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

        // Donor 5: Fiona Dias (O+, ~2.5 km away -> COMPATIBLE WITH A+, B+, AB+, O+)
        User fionaUser = createDemoUser("Fiona Dias", "fiona.donor@lifelink.com", "Donor123!", Role.DONOR);
        DonorProfile fionaProfile = createDemoDonor(fionaUser, "O+",
                "Sector 2, HSR Layout", "Bengaluru", 12.9121, 77.6446, "+91-9876543215", true);

        // Donor 6: George Thomas (B-, ~3.0 km away -> Rh Negative)
        User georgeUser = createDemoUser("George Thomas", "george.donor@lifelink.com", "Donor123!", Role.DONOR);
        createDemoDonor(georgeUser, "B-",
                "Intermediate Ring Road, Domlur", "Bengaluru", 12.9609, 77.6387, "+91-9876543216", true);

        // Donor 7: Hannah Paul (AB-, ~5.0 km away -> Rare blood type)
        User hannahUser = createDemoUser("Hannah Paul", "hannah.donor@lifelink.com", "Donor123!", Role.DONOR);
        createDemoDonor(hannahUser, "AB-",
                "MG Road, Brigade Junction", "Bengaluru", 12.9756, 77.6066, "+91-9876543217", true);

        // Donor 8: Ian Wright (A-, ~1.2 km away -> Compatible with A+, A-)
        User ianUser = createDemoUser("Ian Wright", "ian.donor@lifelink.com", "Donor123!", Role.DONOR);
        createDemoDonor(ianUser, "A-",
                "80ft Road, Ejipura", "Bengaluru", 12.9385, 77.6315, "+91-9876543218", true);

        // Donor 9: Jack Miller (A+, ~3.2 km away -> UNAVAILABLE: Tests availability toggle filter)
        User jackUser = createDemoUser("Jack Miller", "jack.donor@lifelink.com", "Donor123!", Role.DONOR);
        createDemoDonor(jackUser, "A+",
                "16th Main, BTM Layout 2nd Stage", "Bengaluru", 12.9166, 77.6101, "+91-9876543219", false);

        // Donor 10: Emma Watson (A+, ~60 km away in Tumkur -> OUTSIDE 20 KM RADIUS: Tests geo-filter & escalation)
        User emmaUser = createDemoUser("Emma Watson", "emma.donor@lifelink.com", "Donor123!", Role.DONOR);
        createDemoDonor(emmaUser, "A+",
                "5 Bypass Road", "Tumkur", 13.3400, 77.1000, "+91-9876543214", true);

        // ====================================================================
        // 3. Demo Emergency Requests & Initial Donor Matches
        // ====================================================================
        // Request 1: City Care Hospital needs A+ blood (Critical, 2 units)
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

        // Request 2: Metro Trauma Center needs O+ blood (High, 1 unit)
        EmergencyRequest metroReq = new EmergencyRequest();
        metroReq.setHospital(metroProfile);
        metroReq.setBloodGroup("O+");
        metroReq.setUnitsRequired(1);
        metroReq.setUrgency(Urgency.HIGH);
        metroReq.setStatus(RequestStatus.ACTIVE);
        metroReq.setCreatedAt(LocalDateTime.now().minusMinutes(8));
        metroReq = emergencyRequestRepository.save(metroReq);

        // Match 3: Fiona matched for Request 2
        RequestDonorMatch fionaMatch = new RequestDonorMatch();
        fionaMatch.setEmergencyRequest(metroReq);
        fionaMatch.setDonor(fionaProfile);
        fionaMatch.setStatus(MatchStatus.MATCHED);
        fionaMatch.setCreatedAt(LocalDateTime.now().minusMinutes(6));
        fionaMatch.setContactSharingConsent(false);
        requestDonorMatchRepository.save(fionaMatch);

        // ====================================================================
        // 4. Sample Notifications
        // ====================================================================
        createNotification(aliceUser, bloodReq, NotificationType.MATCH_FOUND,
                "Urgent Blood Match Found",
                "City Care Hospital in Koramangala urgently needs 2 units of A+ blood. You are ~0.5 km away.");

        createNotification(hospUser, bloodReq, NotificationType.MATCH_ACCEPTED,
                "Donor Accepted Emergency Request",
                "Bob Rao (O-) accepted emergency blood request #" + bloodReq.getId() + " with contact sharing consent.");

        createNotification(fionaUser, metroReq, NotificationType.MATCH_FOUND,
                "Urgent Blood Match Found",
                "Metro Trauma Center in Indiranagar urgently needs 1 unit of O+ blood. You are ~2.5 km away.");

        log.info("[LifeLink Demo] Successfully seeded demo cluster: 3 Hospitals (2 verified, 1 pending), 10 Donors (covering ABO/Rh & availability), 2 Requests, 3 Matches.");
    }

    private HospitalProfile createDemoHospital(String name, String email, String password,
                                               String regNo, String address, String city,
                                               double lat, double lng, String phone, boolean verified) {
        User user = createDemoUser(name, email, password, Role.HOSPITAL);
        HospitalProfile profile = new HospitalProfile();
        profile.setUser(user);
        profile.setHospitalName(name);
        profile.setRegistrationNumber(regNo);
        profile.setAddress(address);
        profile.setCity(city);
        profile.setLatitude(lat);
        profile.setLongitude(lng);
        profile.setContactNumber(phone);
        profile.setVerified(verified);
        return hospitalProfileRepository.save(profile);
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
