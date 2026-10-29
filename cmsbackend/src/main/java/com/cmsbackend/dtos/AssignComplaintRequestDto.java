
package com.cmsbackend.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignComplaintRequestDto {

    @NotNull
    @Positive
    private Integer officerId;

    @Size(max = 2000)
    private String remarks;
}
