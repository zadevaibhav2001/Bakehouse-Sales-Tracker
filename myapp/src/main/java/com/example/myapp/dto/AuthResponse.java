package com.example.myapp.dto;

public record AuthResponse(String token, String username, String role) {
}