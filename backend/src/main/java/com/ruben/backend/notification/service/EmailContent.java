package com.ruben.backend.notification.service;

/** A plain-text email ready to send. */
public record EmailContent(String to, String subject, String body) {}
