package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateDepartmentRequest {

    @NotBlank @Size(min = 2, max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    private Integer headOfDeptId;

    // Optional: ACTIVE / INACTIVE
    private String status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getHeadOfDeptId() { return headOfDeptId; }
    public void setHeadOfDeptId(Integer headOfDeptId) { this.headOfDeptId = headOfDeptId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
