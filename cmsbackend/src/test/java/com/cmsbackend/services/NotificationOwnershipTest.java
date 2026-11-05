package com.cmsbackend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cmsbackend.entities.Complaint;
import com.cmsbackend.entities.Notification;
import com.cmsbackend.entities.User;
import com.cmsbackend.repositories.NotificationRepository;
import com.cmsbackend.repositories.UserRepository;
import com.cmsbackend.services.impl.NotificationServiceImpl;

class NotificationOwnershipTest {
    private final NotificationRepository notifications = mock(NotificationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final NotificationServiceImpl service = new NotificationServiceImpl(notifications, users);

    private User owner() {
        User user = new User();
        user.setUserId(7);
        user.setEmail("fixture@example.test");
        when(users.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void inboxAndUnreadCountAreOwnerScopedAndIncludeComplaintReference() {
        User user = owner();
        Complaint complaint = new Complaint();
        complaint.setComplaintId(17);
        Notification notification = new Notification();
        notification.setNotificationId(5);
        notification.setUser(user);
        notification.setComplaint(complaint);
        notification.setTitle("Fixture update");
        when(notifications.findByUser_UserIdOrderByCreatedAtDesc(7)).thenReturn(List.of(notification));
        when(notifications.countByUser_UserIdAndIsReadFalse(7)).thenReturn(1L);

        assertEquals(17, service.getMyNotifications(user.getEmail()).get(0).getComplaintId());
        assertEquals(1L, service.getUnreadCount(user.getEmail()));
        verify(notifications).findByUser_UserIdOrderByCreatedAtDesc(7);
        verify(notifications).countByUser_UserIdAndIsReadFalse(7);
    }

    @Test
    void markingAnotherUsersNotificationDoesNotRevealIt() {
        User user = owner();
        when(notifications.findByNotificationIdAndUser_UserId(5, 7)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
                () -> service.markAsRead(5, user.getEmail())).getStatusCode());
    }

    @Test
    void markOneAndMarkAllOnlyChangeOwnerRecords() {
        User user = owner();
        Notification first = new Notification();
        first.setNotificationId(5);
        Notification second = new Notification();
        second.setNotificationId(6);
        when(notifications.findByNotificationIdAndUser_UserId(5, 7)).thenReturn(Optional.of(first));
        when(notifications.findByUser_UserIdOrderByCreatedAtDesc(7)).thenReturn(List.of(first, second));

        assertTrue(service.markAsRead(5, user.getEmail()).getIsRead());
        service.markAllAsRead(user.getEmail());
        assertTrue(first.getIsRead());
        assertTrue(second.getIsRead());
        verify(notifications, never()).findAll();
    }
}
