package com.ruben.backend.common.security;

import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** The authenticated principal: a snapshot of the user taken when the request was authenticated. */
public class TripMateUserDetails implements UserDetails {

  private final Long id;
  private final String name;
  private final String email;
  private final String password;
  private final Role role;
  private final UserStatus status;
  private final UUID avatarId;
  private final LocalDate registrationDate;

  public TripMateUserDetails(User user) {
    this.id = user.getId();
    this.name = user.getName();
    this.email = user.getEmail();
    this.password = user.getPassword();
    this.role = user.getRole();
    this.status = user.getStatus();
    // Reading the id of the lazy avatar does not load the image.
    this.avatarId = user.getAvatar() == null ? null : user.getAvatar().getId();
    this.registrationDate = user.getRegistrationDate();
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Role getRole() {
    return role;
  }

  public UUID getAvatarId() {
    return avatarId;
  }

  public LocalDate getRegistrationDate() {
    return registrationDate;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonLocked() {
    return status != UserStatus.BLOCKED;
  }
}
