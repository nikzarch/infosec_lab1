package org.example.lab1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_.-]{3,64}",
                message = "Username must be 3-64 characters: letters, digits, '.', '_' or '-'")
        String username,
        @NotBlank
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
        @Pattern(regexp = "[\\x20-\\x7E]{12,72}",
                message = "Password must use printable ASCII characters to respect BCrypt's 72-byte limit")
        String password) {
}
