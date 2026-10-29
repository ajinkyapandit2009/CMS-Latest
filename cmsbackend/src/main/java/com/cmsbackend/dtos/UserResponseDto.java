
package com.cmsbackend.dtos;

import java.time.LocalDateTime;

import com.cmsbackend.enums.Role;

import lombok.Data;

@Data
public class UserResponseDto {

    private Integer userId;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String address;

    private Role role;

    private LocalDateTime createdAt;
}
