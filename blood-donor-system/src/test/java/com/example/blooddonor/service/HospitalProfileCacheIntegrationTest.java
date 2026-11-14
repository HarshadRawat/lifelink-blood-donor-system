package com.example.blooddonor.service;

import com.example.blooddonor.dto.HospitalProfileRequest;
import com.example.blooddonor.dto.HospitalProfileResponse;
import com.example.blooddonor.entity.HospitalProfile;
import com.example.blooddonor.entity.Role;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.repository.HospitalProfileRepository;
import com.example.blooddonor.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@Testcontainers(disabledWithoutDocker = true)
class HospitalProfileCacheIntegrationTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.cache.type", () -> "redis");
    }

    @Autowired
    private HospitalProfileService hospitalProfileService;

    @Autowired
    private HospitalProfileRepository hospitalProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private User testUser;
    private HospitalProfile testProfile;

    @BeforeEach
    void setUp() {
        // Clear database
        hospitalProfileRepository.deleteAll();
        userRepository.deleteAll();

        // Atomically flush Redis test container state
        if (redisTemplate.getConnectionFactory() != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        }

        // Create initial test user and hospital profile
        testUser = new User();
        testUser.setName("Apollo Hospital User");
        testUser.setEmail("apollo@test.com");
        testUser.setPassword("Password123!");
        testUser.setRole(Role.HOSPITAL);
        testUser = userRepository.save(testUser);

        testProfile = new HospitalProfile();
        testProfile.setUser(testUser);
        testProfile.setHospitalName("Apollo Hospital");
        testProfile.setRegistrationNumber("APOLLO-101");
        testProfile.setAddress("Greams Road");
        testProfile.setCity("Chennai");
        testProfile.setLatitude(13.0604);
        testProfile.setLongitude(80.2496);
        testProfile.setContactNumber("04428290200");
        testProfile = hospitalProfileRepository.save(testProfile);
    }

    @Test
    @DisplayName("@Cacheable caches profile on first read; subsequent reads hit Redis without querying DB")
    void testGetProfileIsCachedInRedis() {
        Long userId = testUser.getId();

        // 1st invocation: Cache miss -> populates Redis cache
        HospitalProfileResponse firstCall = hospitalProfileService.getProfile(userId);
        assertNotNull(firstCall);
        assertEquals("Apollo Hospital", firstCall.getHospitalName());

        // Verify Redis cache contains the item
        Cache cache = cacheManager.getCache("hospital_profiles");
        assertNotNull(cache, "hospital_profiles cache must exist");
        Cache.ValueWrapper wrapper = cache.get(userId);
        assertNotNull(wrapper, "Value should be cached under key userId=" + userId);
        assertNotNull(wrapper.get());

        // Modify the database record directly behind Spring's back
        testProfile.setHospitalName("Direct DB Edit Should Not Be Seen");
        hospitalProfileRepository.save(testProfile);

        // 2nd invocation: Cache hit -> served from Redis, DB modification ignored!
        HospitalProfileResponse secondCall = hospitalProfileService.getProfile(userId);
        assertNotNull(secondCall);
        assertEquals("Apollo Hospital", secondCall.getHospitalName(),
                "Subsequent read must be served from Redis cache, not the directly modified DB");

        // Verify Redis key TTL is configured (15 minutes = 900 seconds)
        Set<String> keys = redisTemplate.keys("lifelink:cache:hospital_profiles::" + userId);
        assertNotNull(keys);
        assertFalse(keys.isEmpty(), "Redis cache key with prefix must exist");
        String cacheKey = keys.iterator().next();
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 900, "TTL must be between 1 and 900 seconds, but was: " + ttl);
    }

    @Test
    @DisplayName("@CacheEvict evicts cache on update, causing next get to fetch fresh data")
    void testUpdateProfileEvictsCache() {
        Long userId = testUser.getId();

        // Initial read: populate cache
        HospitalProfileResponse initial = hospitalProfileService.getProfile(userId);
        assertEquals("Apollo Hospital", initial.getHospitalName());

        Cache cache = cacheManager.getCache("hospital_profiles");
        assertNotNull(cache);
        assertNotNull(cache.get(userId), "Cache should be populated");

        // Update profile via service: triggers @CacheEvict
        HospitalProfileRequest updateRequest = new HospitalProfileRequest();
        updateRequest.setHospitalName("Apollo Super Specialty Hospital");
        updateRequest.setRegistrationNumber("APOLLO-101-UPDATED");
        updateRequest.setAddress("Greams Road New Wing");
        updateRequest.setCity("Chennai");
        updateRequest.setLatitude(13.0604);
        updateRequest.setLongitude(80.2496);
        updateRequest.setContactNumber("04428299999");

        HospitalProfileResponse updated = hospitalProfileService.updateProfile(updateRequest, userId);
        assertEquals("Apollo Super Specialty Hospital", updated.getHospitalName());

        // Next read should fetch fresh updated data from database and cache it
        HospitalProfileResponse fresh = hospitalProfileService.getProfile(userId);
        assertEquals("Apollo Super Specialty Hospital", fresh.getHospitalName());
        assertEquals("APOLLO-101-UPDATED", fresh.getRegistrationNumber());
    }
}
