package com.lankatech.ems.model;

import com.lankatech.ems.enums.ProgramStatus;
import java.time.LocalDate;

public class TrainingProgram {

    private int programId;
    private String title;
    private String trainer;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer departmentId;              // null = company-wide
    private Integer capacity;
    private String description;
    private ProgramStatus status;
    private Integer createdBy;

    public TrainingProgram() {
    }

    public int getProgramId() { return programId; }
    public void setProgramId(int programId) { this.programId = programId; }

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

    public ProgramStatus getStatus() { return status; }
    public void setStatus(ProgramStatus status) { this.status = status; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}
