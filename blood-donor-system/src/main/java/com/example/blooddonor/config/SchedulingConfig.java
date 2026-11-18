package com.example.blooddonor.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Scheduling and Clock Configuration (Task 8).
 * Enables periodic escalation and expiration background workers.
 * Provides a mockable Clock bean for deterministic time-travel testing.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    @ConditionalOnMissingBean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
