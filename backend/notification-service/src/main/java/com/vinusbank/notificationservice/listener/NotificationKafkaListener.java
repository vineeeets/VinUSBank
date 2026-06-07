package com.vinusbank.notificationservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vinusbank.notificationservice.entity.Notification;
import com.vinusbank.notificationservice.event.KycUpdatedEvent;
import com.vinusbank.notificationservice.event.TransactionCompletedEvent;
import com.vinusbank.notificationservice.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class NotificationKafkaListener {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "txn.completed", groupId = "notification-group")
    public void handleTransactionCompleted(String message) {
        log.info("[NOTIFICATION-LISTENER] Received txn.completed event: {}", message);
        try {
            TransactionCompletedEvent event = objectMapper.readValue(message, TransactionCompletedEvent.class);
            
            // Notify Sender
            Notification senderNotification = Notification.builder()
                    .userEmail(event.getSourceAccountEmail())
                    .type("TRANSACTION")
                    .category("ALERT")
                    .title("Transfer Completed")
                    .message(String.format("You have successfully transferred $%s. Reference: %s", 
                            event.getAmount(), event.getReferenceNumber()))
                    .read(false)
                    .sentAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(senderNotification);

            // Notify Receiver (if internal transfer)
            if (event.getDestinationAccountEmail() != null && !event.getDestinationAccountEmail().isEmpty()) {
                Notification receiverNotification = Notification.builder()
                        .userEmail(event.getDestinationAccountEmail())
                        .type("TRANSACTION")
                        .category("ALERT")
                        .title("Funds Received")
                        .message(String.format("You have received $%s. Reference: %s", 
                                event.getAmount(), event.getReferenceNumber()))
                        .read(false)
                        .sentAt(LocalDateTime.now())
                        .build();
                notificationRepository.save(receiverNotification);
            }
            
            log.info("[NOTIFICATION-LISTENER] Transaction notifications saved.");
        } catch (Exception e) {
            log.error("[NOTIFICATION-LISTENER] Error processing txn.completed event", e);
        }
    }

    @KafkaListener(topics = "kyc.updated", groupId = "notification-group")
    public void handleKycUpdated(String message) {
        log.info("[NOTIFICATION-LISTENER] Received kyc.updated event: {}", message);
        try {
            KycUpdatedEvent event = objectMapper.readValue(message, KycUpdatedEvent.class);
            
            Notification notification = Notification.builder()
                    .userEmail(event.getUserEmail())
                    .type("SECURITY")
                    .category("ALERT")
                    .title("KYC Status Updated")
                    .message(String.format("Your KYC status has been updated to: %s", event.getStatus()))
                    .read(false)
                    .sentAt(LocalDateTime.now())
                    .build();
            
            notificationRepository.save(notification);
            log.info("[NOTIFICATION-LISTENER] KYC notification saved.");
        } catch (Exception e) {
            log.error("[NOTIFICATION-LISTENER] Error processing kyc.updated event", e);
        }
    }
}
