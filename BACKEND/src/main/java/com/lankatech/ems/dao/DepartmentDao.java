package com.lankatech.ems.dao;

import com.lankatech.ems.model.Department;
import java.util.List;
import java.util.Optional;

public interface DepartmentDao {

    Department save(Department department);
    Optional<Department> findById(int departmentId);
    Optional<Department> findByName(String name);
    List<Department> findAll();
    void update(Department department);
    void updateStatus(int departmentId, String status);
    boolean existsByName(String name);
    int countVacancies(int departmentId);
    int countTrainingPrograms(int departmentId);

    // Deletes the department and its job positions (call inside a transaction)
    void deleteWithPositions(int departmentId);
}
