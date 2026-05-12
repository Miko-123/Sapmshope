package com.hopesapms.app.modules.notification.service;

import com.hopesapms.app.modules.notification.model.Notification;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.notification.repository.NotificationRepository;
import com.hopesapms.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createNotification(Integer userId, String title, String message, String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            Notification n = Notification.builder()
                    .user(user)
                    .title(title)
                    .message(message)
                    .type(type)
                    .isRead(false)
                    .build();
            notificationRepository.save(n);
        }
    }

    @Transactional
    public void sendBroadcast(String title, String message) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> !u.isDeleted())
                .collect(Collectors.toList());

        List<Notification> notifications = users.stream().map(user -> Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type("SYSTEM_ANNOUNCEMENT")
                .isRead(false)
                .build()).collect(Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getMyNotifications(Integer userId, Pageable pageable) {
        return notificationRepository.findByUser_IdOrderByIsReadAscCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Integer userId) {
        return notificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    }
}