package com.canteen.app.service;

import com.canteen.app.model.Notification;
import com.canteen.app.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Notification sendNotification(String rollNo, String title, String message, String type, String orderNumber, Integer tokenNumber) {
        if (rollNo == null || rollNo.isBlank()) return null;
        Notification notif = new Notification(rollNo.toUpperCase().trim(), title, message, type, orderNumber, tokenNumber);
        return notificationRepository.save(notif);
    }

    public List<Notification> getRecentNotifications(String rollNo) {
        if (rollNo == null || rollNo.isBlank()) return List.of();
        return notificationRepository.findTop10ByStudentRollNoOrderByCreatedAtDesc(rollNo.toUpperCase().trim());
    }

    public long getUnreadCount(String rollNo) {
        if (rollNo == null || rollNo.isBlank()) return 0;
        return notificationRepository.countByStudentRollNoAndIsReadFalse(rollNo.toUpperCase().trim());
    }

    @Transactional
    public void markAllAsRead(String rollNo) {
        if (rollNo == null || rollNo.isBlank()) return;
        List<Notification> list = notificationRepository.findByStudentRollNoOrderByCreatedAtDesc(rollNo.toUpperCase().trim());
        for (Notification n : list) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(list);
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }
}
