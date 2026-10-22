package com.ruben.backend.user;

import com.ruben.backend.user.entity.Role;
import com.ruben.backend.user.entity.User;
import com.ruben.backend.user.entity.UserStatus;
import org.springframework.test.util.ReflectionTestUtils;

/** Builds users with a known id for unit tests, where no database assigns one. */
public final class UserTestData {

  private UserTestData() {}

  public static User user(Long id, String email, Role role, UserStatus status) {
    User user = new User("Test " + role, email, "hash", role);
    user.setStatus(status);
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }
}
