package com.ruben.backend.user.repository;

import com.ruben.backend.user.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmailIgnoreCase(String email);

  /**
   * One page of the regular users whose name or email contains `search`, ignoring case; all of them
   * when `search` is null. The administrator is never listed.
   */
  @Query(
      "select u from User u where u.role = com.ruben.backend.user.entity.Role.USER"
          + " and (:search is null"
          + " or lower(u.name) like lower(concat('%', :search, '%'))"
          + " or lower(u.email) like lower(concat('%', :search, '%')))")
  Page<User> searchUsers(@Param("search") String search, Pageable pageable);

  /** Turns every administrator except the given one into a regular user and returns how many. */
  @Modifying(flushAutomatically = true)
  @Query(
      "update User u set u.role = com.ruben.backend.user.entity.Role.USER"
          + " where u.role = com.ruben.backend.user.entity.Role.ADMIN and u.email <> :adminEmail")
  int demoteOtherAdmins(@Param("adminEmail") String adminEmail);
}
