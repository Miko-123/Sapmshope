package com.hopesapms.app.controller;

import com.hopesapms.app.model.Notification;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.UserRepository;
import com.hopesapms.app.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private Integer getUserId(Authentication auth) {
        return userRepository.findByEmailAndIsDeletedFalse(auth.getName())
                .map(User::getId).orElseThrow();
    }

    @GetMapping
    public ResponseEntity<Page<Notification>> getNotifications(Authentication auth, Pageable pageable) {
        return ResponseEntity.ok(notificationService.getMyNotifications(getUserId(auth), pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication auth) {
        return ResponseEntity.ok(notificationService.getUnreadCount(getUserId(auth)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication auth) {
        notificationService.markAllAsRead(getUserId(auth));
        return ResponseEntity.ok().build();
    }
}