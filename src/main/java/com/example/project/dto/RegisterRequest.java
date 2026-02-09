package com.example.project.dto;

import com.example.project.entity.RoleType;
import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String phoneNumber;
    private String password;
    private RoleType roleType;
}
