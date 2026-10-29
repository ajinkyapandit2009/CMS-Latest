
package com.cmsbackend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CaseOfficerRequestDto {

    @NotNull(message = "User ID is required")
    private Integer userId;

    @Size(max = 100, message = "Department cannot exceed 100 characters")
    private String department;

    @Size(max = 100, message = "Designation cannot exceed 100 characters")
    private String designation;

    @NotBlank(message = "Employee number is required")
    @Size(max = 50, message = "Employee number cannot exceed 50 characters")
    private String employeeNumber;
}
