package com.lankatech.ems.model;

import com.lankatech.ems.enums.VacancyStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Vacancy {

    private int vacancyId;
    private String title;
    private int departmentId;
    private String requirements;
    private LocalDate deadline;
    private VacancyStatus status;
    private int createdBy;                     // user_id of the HR Manager
    private LocalDateTime createdAt;

    public Vacancy() {
    }

    public int getVacancyId() { return vacancyId; }
    public void setVacancyId(int vacancyId) { this.vacancyId = vacancyId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public VacancyStatus getStatus() { return status; }
    public void setStatus(VacancyStatus status) { this.status = status; }

    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
