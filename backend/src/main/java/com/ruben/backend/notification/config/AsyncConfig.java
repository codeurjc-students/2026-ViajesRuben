package com.ruben.backend.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Lets the notification listeners run in the background, so a slow SMTP server never delays a
 * request.
 */
@Configuration
@EnableAsync
public class AsyncConfig {}
