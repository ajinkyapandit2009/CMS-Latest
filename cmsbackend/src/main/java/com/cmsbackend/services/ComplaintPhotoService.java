package com.cmsbackend.services;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import com.cmsbackend.dtos.ComplaintPhotoUploadResponseDto;
import com.cmsbackend.entities.Complaint;

public interface ComplaintPhotoService {
    ComplaintPhotoUploadResponseDto upload(MultipartFile photo, BigDecimal latitude, BigDecimal longitude, BigDecimal accuracy, String email);
    Path getContentPath(UUID uploadId, String email);
    String getContentType(UUID uploadId, String email);
    // Locks the owner's managed upload for this transaction and returns its linked complaint, if any.
    Complaint findComplaintForSubmission(String imageReference, Integer citizenUserId,
            BigDecimal latitude, BigDecimal longitude);
    void consumeForComplaint(String imageReference, Complaint complaint, BigDecimal latitude, BigDecimal longitude, String email);
}
