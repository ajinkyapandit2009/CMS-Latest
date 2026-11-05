package com.cmsbackend.services;

import java.math.BigDecimal;
import java.util.UUID;
import com.cmsbackend.dtos.LocationSharingCreateResponseDto;
import com.cmsbackend.dtos.LocationSharingSessionResponseDto;

public interface LocationSharingService {
    LocationSharingCreateResponseDto create(String email);
    LocationSharingSessionResponseDto get(UUID sessionId, String email);
    void submit(String token, BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy);
    LocationSharingSessionResponseDto cancel(UUID sessionId, String email);
}
