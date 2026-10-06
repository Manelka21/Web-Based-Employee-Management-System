package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.RegisterEmployeeRequest;
import com.lankatech.ems.dto.request.UpdateEmployeeRequest;
import com.lankatech.ems.dto.request.UpdateProfileRequest;
import com.lankatech.ems.model.Employee;

import java.util.List;

public interface EmployeeService {

    Employee register(RegisterEmployeeRequest request, int actorUserId);
    Employee create(Employee employee, int actorUserId);
    Employee getById(int employeeId);
    Employee getForUser(int employeeId, int userId);
    Employee getMyProfile(int userId);
    List<Employee> findAll();
    List<Employee> findByDepartment(int departmentId, int userId);
    Employee update(int employeeId, UpdateEmployeeRequest request, int actorUserId);
    Employee updateMyProfile(int userId, UpdateProfileRequest request);
    void delete(int employeeId, int actorUserId);
}
