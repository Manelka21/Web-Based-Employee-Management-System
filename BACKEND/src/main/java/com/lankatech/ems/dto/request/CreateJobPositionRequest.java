package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateJobPositionRequest {

    @NotBlank @Size(min = 2, max = 120)
    private String title;

    @NotNull
    private Integer departmentId;

    @Size(max = 500)
    private String description;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
