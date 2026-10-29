
package com.cmsbackend.dtos;

import com.cmsbackend.enums.ComplaintPriority;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateComplaintPriorityRequestDto {

    @NotNull(message = "Priority is required")
    private ComplaintPriority priority;
}
