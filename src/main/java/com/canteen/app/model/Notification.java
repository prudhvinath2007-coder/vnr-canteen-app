package com.canteen.app.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "NOTIFICATIONS")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTIFICATION_ID")
    private Long id;

    @Column(name = "STUDENT_ROLL_NO", nullable = false, length = 50)
    private String studentRollNo;

    @Column(name = "TITLE", nullable = false, length = 150)
    private String title;

    @Column(name = "MESSAGE", nullable = false, length = 500)
    private String message;

    @Column(name = "TYPE", length = 30)
    private String type = "INFO"; // PREPARING, READY, DELIVERED, CANCELLED

    @Column(name = "ORDER_NUMBER", length = 50)
    private String orderNumber;

    @Column(name = "TOKEN_NUMBER")
    private Integer tokenNumber;

    @Column(name = "IS_READ")
    private Boolean isRead = false;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Notification() {}

    public Notification(String studentRollNo, String title, String message, String type, String orderNumber, Integer tokenNumber) {
        this.studentRollNo = studentRollNo;
        this.title = title;
        this.message = message;
        this.type = type;
        this.orderNumber = orderNumber;
        this.tokenNumber = tokenNumber;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public String getFormattedTime() {
        if (createdAt == null) return "";
        return createdAt.format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentRollNo() {
        return studentRollNo;
    }

    public void setStudentRollNo(String studentRollNo) {
        this.studentRollNo = studentRollNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(Integer tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean read) {
        isRead = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
