package org.example.lab1.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.example.lab1.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
