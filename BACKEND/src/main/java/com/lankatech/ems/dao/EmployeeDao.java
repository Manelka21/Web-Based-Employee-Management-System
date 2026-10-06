package com.lankatech.ems.dao;

import com.lankatech.ems.model.Employee;
import java.util.List;
import java.util.Optional;

public interface EmployeeDao {

    Employee save(Employee employee);
    Optional<Employee> findById(int employeeId);
    Optional<Employee> findByNic(String nic);
    Optional<Employee> findByEmail(String email);
    List<Employee> findAll();
    List<Employee> findByDepartment(int departmentId);
    void update(Employee employee);
    void updateStatus(int employeeId, String status);
    boolean existsByNic(String nic);
    boolean existsByEmail(String email);
    int countAll();
    int countByStatus(String status);
    int countByDepartment(int departmentId);
    int countByPosition(int positionId);

    // Permanently removes the employee and everything that belongs to them.
    // Call inside a transaction (EmployeeServiceImpl.delete) so it's all-or-nothing.
    void deleteWithHistory(int employeeId);
}
