package com.example.blooddonor.service;

import com.example.blooddonor.dto.NotificationResponse;
import com.example.blooddonor.entity.Notification;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.exception.NotificationAccessDeniedException;
import com.example.blooddonor.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private SimpMessagingTemplate messagingTemplate;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);

        notificationService = new NotificationService(
                notificationRepository,
                messagingTemplate
        );
    }

    @Test
    void markAsRead_shouldRejectUserWhoDoesNotOwnNotification() {

        // Notification belongs to hospital user 8
        User hospital = new User();
        hospital.setId(8L);

        Notification notification = new Notification();
        notification.setId(47L);
        notification.setUser(hospital);
        notification.setRead(false);

        when(notificationRepository.findById(47L))
                .thenReturn(Optional.of(notification));

        // Donor user 14 attempts to read the hospital's notification
        assertThrows(
                NotificationAccessDeniedException.class,
                () -> notificationService.markAsRead(47L, 14L)
        );

        // The notification must remain unread
        assertFalse(notification.isRead());

        // The unauthorized attempt must not save any changes
        verify(notificationRepository, never())
                .save(any(Notification.class));
    }


    @Test
    void markAsRead_shouldAllowOwnerToMarkNotificationAsRead() {
        User hospital = new User();
        hospital.setId(8L);

        Notification notification = new Notification();
        notification.setId(47L);
        notification.setUser(hospital);
        notification.setRead(false);

        when(notificationRepository.findById(47L))
                .thenReturn(Optional.of(notification));

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response =
                notificationService.markAsRead(47L, 8L);

        assertTrue(notification.isRead());
        assertTrue(response.isRead());

        verify(notificationRepository).save(notification);
    }


    @Test
    void getMyNotifications_shouldRetrieveOnlyCurrentUsersNotifications() {
        User donor = new User();
        donor.setId(14L);

        Notification notification = new Notification();
        notification.setId(46L);
        notification.setUser(donor);

        when(notificationRepository.findByUserOrderByCreatedAtDesc(any(User.class)))
                .thenReturn(List.of(notification));

        List<NotificationResponse> responses =
                notificationService.getMyNotifications(14L);

        assertEquals(1, responses.size());
        assertEquals(46L, responses.get(0).getId());

        verify(notificationRepository)
                .findByUserOrderByCreatedAtDesc(
                        argThat(user -> user.getId().equals(14L))
                );
    }


    @Test
    void getUnreadNotificationCount_shouldReturnCountForUser() {
        User donor = new User();
        donor.setId(14L);

        when(notificationRepository.countByUserAndReadFalse(any(User.class)))
                .thenReturn(3L);

        long count = notificationService.getUnreadNotificationCount(14L);

        assertEquals(3L, count);

        verify(notificationRepository)
                .countByUserAndReadFalse(
                        argThat(user -> user.getId().equals(14L))
                );
    }

}