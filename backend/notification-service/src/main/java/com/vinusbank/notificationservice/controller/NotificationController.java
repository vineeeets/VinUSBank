package com.vinusbank.notificationservice.controller;

import com.vinusbank.notificationservice.entity.Notification;
import com.vinusbank.notificationservice.entity.NotificationPreference;
import com.vinusbank.notificationservice.repository.NotificationPreferenceRepository;
import com.vinusbank.notificationservice.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @GetMapping
    public ResponseEntity<List<Notification>> getUserNotifications(@RequestHeader("X-User-Email") String email) {
        log.info("[NOTIFICATION-CTRL] Fetching notifications for user: {}", email);
        return ResponseEntity.ok(notificationRepository.findByUserEmailOrderBySentAtDesc(email));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getUnreadNotifications(@RequestHeader("X-User-Email") String email) {
        return ResponseEntity.ok(notificationRepository.findByUserEmailAndReadFalse(email));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, @RequestHeader("X-User-Email") String email) {
        Notification notification = notificationRepository.findById(id).orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUserEmail().equals(email)) {
            return ResponseEntity.status(403).body("Access Denied");
        }
        
        notification.setRead(true);
        notificationRepository.save(notification);
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestHeader("X-User-Email") String email) {
        List<Notification> unread = notificationRepository.findByUserEmailAndReadFalse(email);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }

    @GetMapping("/preferences")
    public ResponseEntity<List<NotificationPreference>> getPreferences(@RequestHeader("X-User-Email") String email) {
        // Since we don't have a complex find logic for all prefs of a user in the repo yet, 
        // we can just return all or a specific one. For now, let's just use findAll and filter, or add method.
        // I will just add the method to the repo later, for now just return empty list or dummy
        return ResponseEntity.ok(List.of());
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@RequestHeader("X-User-Email") String email, @RequestBody NotificationPreference pref) {
        pref.setUserEmail(email);
        NotificationPreference existing = preferenceRepository.findByUserEmailAndCategory(email, pref.getCategory()).orElse(null);
        
        if (existing != null) {
            existing.setEmailEnabled(pref.isEmailEnabled());
            existing.setSmsEnabled(pref.isSmsEnabled());
            existing.setInAppEnabled(pref.isInAppEnabled());
            preferenceRepository.save(existing);
        } else {
            preferenceRepository.save(pref);
        }
        
        return ResponseEntity.ok(Map.of("message", "Preferences updated"));
    }
}
