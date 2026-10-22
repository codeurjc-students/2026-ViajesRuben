package com.ruben.backend.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Turns a valid token into the current user. The user is loaded from the database on every request
 * so that a user blocked after logging in is rejected immediately, and the authorities always come
 * from the stored role, never from the token.
 */
@Component
public class ActiveUserJwtConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

  private final TripMateUserDetailsService userDetailsService;

  public ActiveUserJwtConverter(TripMateUserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @Override
  public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
    TripMateUserDetails user;
    try {
      user = userDetailsService.loadUserById(Long.valueOf(jwt.getSubject()));
    } catch (NumberFormatException | UsernameNotFoundException e) {
      throw new BadCredentialsException("Invalid token subject", e);
    }
    if (!user.isAccountNonLocked()) {
      throw new LockedException("Account is blocked");
    }
    return UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
  }
}
