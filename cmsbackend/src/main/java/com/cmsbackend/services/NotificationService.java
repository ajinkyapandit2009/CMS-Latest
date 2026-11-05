package com.cmsbackend.services;

import java.util.List;
import com.cmsbackend.dtos.NotificationResponseDto;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.User;

public interface NotificationService {
    void createNotification(User user, Complaint complaint, String title, String message);
    List<NotificationResponseDto> getMyNotifications(String email);
    long getUnreadCount(String email);
    NotificationResponseDto markAsRead(Integer notificationId, String email);
    void markAllAsRead(String email);
}
