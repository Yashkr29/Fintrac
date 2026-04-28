package com.fintrac.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type;
    private Long userId;
    private String username;
    private String email;

    public AuthResponse(String token, Long userId, String username, String email) {
        this.token = token;
        this.type = "Bearer";
        this.userId = userId;
        this.username = username;
        this.email = email;
    }
}
