package com.lankatech.ems.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PerformanceRecord {

    private int performanceId;
    private int employeeId;
    private int supervisorId;                  // user_id of the Department Supervisor
    private String feedback;
    private Integer rating;                    // 1–5 scale, nullable
    private LocalDate reviewDate;
    private LocalDateTime createdAt;

    public PerformanceRecord() {
    }

    public int getPerformanceId() { return performanceId; }
    public void setPerformanceId(int performanceId) { this.performanceId = performanceId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public int getSupervisorId() { return supervisorId; }
    public void setSupervisorId(int supervisorId) { this.supervisorId = supervisorId; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public LocalDate getReviewDate() { return reviewDate; }
    public void setReviewDate(LocalDate reviewDate) { this.reviewDate = reviewDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
