package com.cmsbackend.entities;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "complaint_photo_uploads")
public class ComplaintPhotoUpload {
    @Id @JdbcTypeCode(Types.CHAR) @Column(name = "upload_id", length = 36) private UUID uploadId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(name = "storage_key", nullable = false, unique = true, length = 255) private String storageKey;
    @Column(name = "mime_type", nullable = false, length = 100) private String mimeType;
    @Column(name = "file_size", nullable = false) private Long fileSize;
    @Column(nullable = false, precision = 10, scale = 8) private BigDecimal latitude;
    @Column(nullable = false, precision = 11, scale = 8) private BigDecimal longitude;
    @Column(name = "accuracy_meters", nullable = false, precision = 10, scale = 2) private BigDecimal accuracyMeters;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "expires_at") private LocalDateTime expiresAt;
    @Column(name = "consumed_at") private LocalDateTime consumedAt;
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "complaint_id") private Complaint complaint;
}
