package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class CreateTrainingProgramRequest {

    @NotBlank @Size(min = 3, max = 120)
    private String title;

    @Size(max = 120)
    private String trainer;

    private LocalDate startDate;
    private LocalDate endDate;

    // null = company-wide program
    private Integer departmentId;

    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 500, message = "Capacity cannot be more than 500")
    private Integer capacity;

    @Size(max = 500)
    private String description;

    // Optional on update: SCHEDULED / IN_PROGRESS / COMPLETED / CANCELLED
    private String status;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTrainer() { return trainer; }
    public void setTrainer(String trainer) { this.trainer = trainer; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
