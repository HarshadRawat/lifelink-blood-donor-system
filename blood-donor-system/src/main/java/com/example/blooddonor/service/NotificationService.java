package com.example.blooddonor.service;

import com.example.blooddonor.dto.NotificationResponse;
import com.example.blooddonor.entity.EmergencyRequest;
import com.example.blooddonor.entity.Notification;
import com.example.blooddonor.entity.NotificationType;
import com.example.blooddonor.entity.User;
import com.example.blooddonor.exception.NotificationAccessDeniedException;
import com.example.blooddonor.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public Notification createMatchFoundNotification(
            User donor,
            EmergencyRequest emergencyRequest
    ) {

        Notification notification = new Notification();

        notification.setUser(donor);
        notification.setType(NotificationType.MATCH_FOUND);
        notification.setTitle("Emergency Blood Request");
        notification.setMessage(
                "You have been matched with an emergency blood request for "
                        + emergencyRequest.getBloodGroup()
                        + " blood."
        );
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notification.setEmergencyRequest(emergencyRequest);

        Notification savedNotification =
                notificationRepository.save(notification);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(donor.getId()),
                "/queue/notifications",
                mapToResponse(savedNotification)
        );

        return savedNotification;
    }

    public List<NotificationResponse> getMyNotifications(Long userId) {

        User user = new User();
        user.setId(userId);

        return notificationRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public NotificationResponse markAsRead(Long notificationId, Long userId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new NotificationAccessDeniedException(
                    "You are not allowed to access this notification"
            );
        }

        notification.setRead(true);

        return mapToResponse(notificationRepository.save(notification));
    }

    private NotificationResponse mapToResponse(Notification notification) {

        NotificationResponse response = new NotificationResponse();

        response.setId(notification.getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setMessage(notification.getMessage());
        response.setRead(notification.isRead());
        response.setCreatedAt(notification.getCreatedAt());

        if (notification.getEmergencyRequest() != null) {
            response.setEmergencyRequestId(
                    notification.getEmergencyRequest().getId()
            );
        }

        return response;
    }

    public Notification createMatchAcceptedNotification(
            User hospitalUser,
            EmergencyRequest emergencyRequest,
            User donorUser
    ) {

        Notification notification = new Notification();

        notification.setUser(hospitalUser);
        notification.setType(NotificationType.MATCH_ACCEPTED);
        notification.setTitle("Donor Accepted Request");
        notification.setMessage(
                donorUser.getName()
                        + " has accepted your emergency blood request for "
                        + emergencyRequest.getBloodGroup()
                        + " blood."
        );
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notification.setEmergencyRequest(emergencyRequest);

        // Save the notification to the database
        Notification savedNotification =
                notificationRepository.save(notification);

        // Send the notification to the hospital in real time
        messagingTemplate.convertAndSendToUser(
                String.valueOf(hospitalUser.getId()),
                "/queue/notifications",
                mapToResponse(savedNotification)
        );

        return savedNotification;
    }

    public Notification createMatchRejectedNotification(
            User hospitalUser,
            EmergencyRequest emergencyRequest,
            User donorUser
    ) {

        Notification notification = new Notification();

        notification.setUser(hospitalUser);
        notification.setType(NotificationType.MATCH_REJECTED);
        notification.setTitle("Donor Rejected Request");
        notification.setMessage(
                donorUser.getName()
                        + " has rejected your emergency blood request for "
                        + emergencyRequest.getBloodGroup()
                        + " blood."
        );
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notification.setEmergencyRequest(emergencyRequest);

        // Save the notification to the database
        Notification savedNotification =
                notificationRepository.save(notification);

        // Send the notification to the hospital in real time
        messagingTemplate.convertAndSendToUser(
                String.valueOf(hospitalUser.getId()),
                "/queue/notifications",
                mapToResponse(savedNotification)
        );

        return savedNotification;
    }

    public Notification createRequestFulfilledNotification(
            User hospitalUser,
            EmergencyRequest emergencyRequest
    ) {

        Notification notification = new Notification();

        notification.setUser(hospitalUser);
        notification.setType(NotificationType.REQUEST_FULFILLED);
        notification.setTitle("Emergency Request Fulfilled");
        notification.setMessage(
                "Your emergency blood request for "
                        + emergencyRequest.getBloodGroup()
                        + " blood has been fulfilled."
        );
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notification.setEmergencyRequest(emergencyRequest);

        // Save the notification to the database
        Notification savedNotification =
                notificationRepository.save(notification);

        // Send the notification to the hospital in real time
        messagingTemplate.convertAndSendToUser(
                String.valueOf(hospitalUser.getId()),
                "/queue/notifications",
                mapToResponse(savedNotification)
        );

        return savedNotification;
    }

    public Notification createRequestCancelledNotification(
            User donorUser,
            EmergencyRequest emergencyRequest
    ) {

        Notification notification = new Notification();

        notification.setUser(donorUser);
        notification.setType(NotificationType.REQUEST_CANCELLED);
        notification.setTitle("Emergency Request Cancelled");
        notification.setMessage(
                "The emergency blood request for "
                        + emergencyRequest.getBloodGroup()
                        + " blood has been cancelled."
        );
        notification.setRead(false);
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notification.setEmergencyRequest(emergencyRequest);

        // Save notification in the database
        Notification savedNotification =
                notificationRepository.save(notification);

        // Send real-time notification to the specific donor
        messagingTemplate.convertAndSendToUser(
                String.valueOf(donorUser.getId()),
                "/queue/notifications",
                mapToResponse(savedNotification)
        );

        return savedNotification;
    }

    public long getUnreadNotificationCount(Long userId) {
        User user = new User();
        user.setId(userId);

        return notificationRepository.countByUserAndReadFalse(user);
    }
}