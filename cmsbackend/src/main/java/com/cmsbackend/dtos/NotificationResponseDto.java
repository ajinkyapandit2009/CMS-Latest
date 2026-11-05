package com.cmsbackend.dtos;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class NotificationResponseDto {
    private Integer notificationId;
    private Integer complaintId;
    private String title;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
