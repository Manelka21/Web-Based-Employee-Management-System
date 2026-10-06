package com.lankatech.ems.model;

public class Department {

    private int departmentId;
    private String name;
    private String description;
    private Integer headOfDeptId;     // nullable: references an employee_id
    private String status;             // ACTIVE / INACTIVE

    public Department() {
    }

    public Department(String name, String description) {
        this.name = name;
        this.description = description;
        this.status = "ACTIVE";
    }

    public int getDepartmentId() { return departmentId; }
    public void setDepartmentId(int departmentId) { this.departmentId = departmentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getHeadOfDeptId() { return headOfDeptId; }
    public void setHeadOfDeptId(Integer headOfDeptId) { this.headOfDeptId = headOfDeptId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
