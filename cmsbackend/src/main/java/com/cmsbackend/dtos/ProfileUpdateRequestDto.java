package com.cmsbackend.dtos;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequestDto(
        @Size(max = 15, message = "Phone must be 15 characters or fewer")
        @Pattern(regexp = "^\\s*$|^(?=.*\\d)[+0-9() .-]+$",
                message = "Phone must contain a digit and use only phone characters")
        String phone,
        @Size(max = 65535, message = "Address is too long")
        String address) {
}
