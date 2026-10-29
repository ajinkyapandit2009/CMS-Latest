
package com.cmsbackend.dtos;

import com.cmsbackend.enums.ComplaintStatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateComplaintStatusRequestDto {

    @NotNull
    private ComplaintStatus status;

    @Size(max = 2000)
    private String remarks;
}
