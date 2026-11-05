package com.cmsbackend.dtos;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationSharingCreateResponseDto {
	private UUID sessionId;
	private String shareUrl;
	private LocalDateTime expiresAt;
}
