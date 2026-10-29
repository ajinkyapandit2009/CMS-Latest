
package com.cmsbackend.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class CaseOfficerRequestDto {

    @NotNull(message = "User ID is required")
    private Integer userId;
}
