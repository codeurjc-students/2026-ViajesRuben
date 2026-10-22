package com.ruben.backend.common.security;

import com.ruben.backend.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class TripMateUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public TripMateUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /** Used at login: the username is the email. */
  @Override
  public TripMateUserDetails loadUserByUsername(String email) {
    return userRepository
        .findByEmail(email)
        .map(TripMateUserDetails::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  /** Used on every authenticated request: the token subject is the user id. */
  public TripMateUserDetails loadUserById(Long id) {
    return userRepository
        .findById(id)
        .map(TripMateUserDetails::new)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
