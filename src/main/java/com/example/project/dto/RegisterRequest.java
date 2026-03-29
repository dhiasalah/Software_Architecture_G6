package com.example.project.dto;

import com.example.project.entity.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String phoneNumber;
    private String password;

    @Schema(hidden = true)
    private RoleType roleType;
}
