package com.cmsbackend.repositories;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.cmsbackend.entities.ComplaintPhotoUpload;
public interface ComplaintPhotoUploadRepository extends JpaRepository<ComplaintPhotoUpload, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ComplaintPhotoUpload> findByUploadIdAndUser_UserId(UUID uploadId, Integer userId);
}
