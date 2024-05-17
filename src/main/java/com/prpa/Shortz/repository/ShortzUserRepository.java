package com.prpa.Shortz.repository;

import com.prpa.Shortz.model.ShortzUser;
import com.prpa.Shortz.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortzUserRepository extends JpaRepository<ShortzUser, Long> {

    boolean existsByRole(Role role);

    Optional<ShortzUser> findByUsernameOrEmail(String username, String email);

}