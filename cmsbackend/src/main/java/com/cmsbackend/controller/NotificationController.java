package com.cmsbackend.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.cmsbackend.dtos.NotificationResponseDto;
import com.cmsbackend.services.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;
    public NotificationController(NotificationService notificationService) { this.notificationService = notificationService; }
    @GetMapping("/my") public ResponseEntity<List<NotificationResponseDto>> getMyNotifications(Authentication authentication) { return ResponseEntity.ok(notificationService.getMyNotifications(authentication.getName())); }
    @GetMapping("/unread-count") public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) { return ResponseEntity.ok(Map.of("unreadCount", notificationService.getUnreadCount(authentication.getName()))); }
    @PatchMapping("/{notificationId}/read") public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable Integer notificationId, Authentication authentication) { return ResponseEntity.ok(notificationService.markAsRead(notificationId, authentication.getName())); }
    @PatchMapping("/read-all") public ResponseEntity<Void> markAllAsRead(Authentication authentication) { notificationService.markAllAsRead(authentication.getName()); return ResponseEntity.noContent().build(); }
}
