package com.vinusbank.notificationservice.repository;

import com.vinusbank.notificationservice.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
    Optional<NotificationPreference> findByUserEmailAndCategory(String userEmail, String category);
}
