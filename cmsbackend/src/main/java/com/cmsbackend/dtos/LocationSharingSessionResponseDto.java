package com.cmsbackend.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.cmsbackend.enums.LocationSharingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationSharingSessionResponseDto {
    private UUID sessionId;
    private LocationSharingStatus status;
    private LocalDateTime expiresAt;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracy;
}
