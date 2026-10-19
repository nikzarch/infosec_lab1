package org.example.lab1.dto;

import java.time.Instant;
import org.example.lab1.model.User;

/** Public representation; intentionally excludes password hashes and internal fields. */
public record UserResponse(Long id, String username, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }
}
