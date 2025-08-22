package com.akiramenai.backend.repo;

import com.akiramenai.backend.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<Users, UUID> {
  Optional<Users> findUsersByUsername(String username);

  Optional<Users> findUsersById(UUID id);

  Optional<Users> findUsersByEmail(String email);

  Optional<Users> getUsersByEmail(String email);
}
