package com.cmsbackend.dtos;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;
@Data
public class ComplaintPhotoUploadResponseDto {
    private UUID uploadId;
    private String imageUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private BigDecimal accuracy;
}
