package com.example.blooddonor.dto;

import com.example.blooddonor.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserSummary {
    private Long id;
    private String name;
    private String email;
    private Role role;
}
