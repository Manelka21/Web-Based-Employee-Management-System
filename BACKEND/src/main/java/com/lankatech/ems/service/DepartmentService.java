package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.CreateDepartmentRequest;
import com.lankatech.ems.model.Department;

import java.util.List;

public interface DepartmentService {

    Department create(CreateDepartmentRequest request, int actorUserId);
    List<Department> findAll();
    Department findById(int departmentId);
    Department update(int departmentId, CreateDepartmentRequest request, int actorUserId);
    void delete(int departmentId, int actorUserId);
}
