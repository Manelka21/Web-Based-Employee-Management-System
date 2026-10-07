package com.lankatech.ems.model;

public class JobPosition {

    private int positionId;
    private String title;
    private int departmentId;
    private String description;

    public JobPosition() {
    }

    public int getPositionId() { return positionId; }
    public void setPositionId(int positionId) { this.positionId = positionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
