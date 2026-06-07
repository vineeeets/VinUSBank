package com.vinusbank.notificationservice.repository;

import com.vinusbank.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderBySentAtDesc(String userEmail);
    List<Notification> findByUserEmailAndReadFalse(String userEmail);
}
