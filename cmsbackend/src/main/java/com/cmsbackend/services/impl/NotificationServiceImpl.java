package com.cmsbackend.services.impl;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.dtos.NotificationResponseDto;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.Notification;
import com.cmsbackend.entities.User;
import com.cmsbackend.repositories.NotificationRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) { this.notificationRepository = notificationRepository; this.userRepository = userRepository; }
    @Override public void createNotification(User user, Complaint complaint, String title, String message) { Notification notification = new Notification(); notification.setUser(user); notification.setComplaint(complaint); notification.setTitle(title); notification.setMessage(message); notification.setIsRead(false); notificationRepository.save(notification); }
    @Override @Transactional(readOnly = true) public List<NotificationResponseDto> getMyNotifications(String email) { return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUser(email).getUserId()).stream().map(this::toDto).toList(); }
    @Override @Transactional(readOnly = true) public long getUnreadCount(String email) { return notificationRepository.countByUser_UserIdAndIsReadFalse(currentUser(email).getUserId()); }
    @Override @Transactional public NotificationResponseDto markAsRead(Integer notificationId, String email) { Notification notification = notificationRepository.findByNotificationIdAndUser_UserId(notificationId, currentUser(email).getUserId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found")); notification.setIsRead(true); return toDto(notification); }
    @Override @Transactional public void markAllAsRead(String email) { notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(currentUser(email).getUserId()).stream().filter(notification -> !Boolean.TRUE.equals(notification.getIsRead())).forEach(notification -> notification.setIsRead(true)); }
    private User currentUser(String email) { return userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found")); }
    private NotificationResponseDto toDto(Notification notification) { NotificationResponseDto dto = new NotificationResponseDto(); dto.setNotificationId(notification.getNotificationId()); dto.setComplaintId(notification.getComplaint() == null ? null : notification.getComplaint().getComplaintId()); dto.setTitle(notification.getTitle()); dto.setMessage(notification.getMessage()); dto.setIsRead(notification.getIsRead()); dto.setCreatedAt(notification.getCreatedAt()); return dto; }
}
