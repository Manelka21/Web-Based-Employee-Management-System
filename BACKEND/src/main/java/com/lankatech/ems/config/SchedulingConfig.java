package com.lankatech.ems.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Turns on @Scheduled jobs (LeaveStatusJob, NotificationDispatcher)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
