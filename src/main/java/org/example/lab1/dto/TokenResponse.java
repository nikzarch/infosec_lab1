package org.example.lab1.dto;

public record TokenResponse(String tokenType, String accessToken, long expiresIn) {
}
