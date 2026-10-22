package com.ruben.backend.user.dto;

/**
 * Outcome of a successful login: the user to return and the token the controller puts in a cookie.
 */
public record LoginResult(UserDTO user, String token) {}
