package com.lankatech.ems.dao;

import com.lankatech.ems.model.EmployeeSalary;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EmployeeSalaryDao {

    Optional<EmployeeSalary> findByEmployee(int employeeId);
    List<EmployeeSalary> findAll();
    void upsert(int employeeId, BigDecimal baseSalary, int updatedBy);
}
