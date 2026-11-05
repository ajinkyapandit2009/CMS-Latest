package com.cmsbackend.controller;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.cmsbackend.dtos.ComplaintPhotoUploadResponseDto;
import com.cmsbackend.services.ComplaintPhotoService;

@RestController
@RequestMapping("/api/complaint-photos")
public class ComplaintPhotoController {
    private final ComplaintPhotoService photoService;
    public ComplaintPhotoController(ComplaintPhotoService photoService) { this.photoService = photoService; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplaintPhotoUploadResponseDto> upload(@RequestParam("photo") MultipartFile photo, @RequestParam BigDecimal latitude, @RequestParam BigDecimal longitude, @RequestParam BigDecimal accuracy, Authentication authentication) { return ResponseEntity.ok(photoService.upload(photo, latitude, longitude, accuracy, authentication.getName())); }
    @GetMapping("/{uploadId}/content")
    public ResponseEntity<FileSystemResource> content(@PathVariable UUID uploadId, Authentication authentication) { String mime = photoService.getContentType(uploadId, authentication.getName()); Path path = photoService.getContentPath(uploadId, authentication.getName()); return ResponseEntity.ok().contentType(MediaType.parseMediaType(mime)).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename("complaint-photo").build().toString()).header(HttpHeaders.CACHE_CONTROL, "private, no-store").body(new FileSystemResource(path)); }
}
