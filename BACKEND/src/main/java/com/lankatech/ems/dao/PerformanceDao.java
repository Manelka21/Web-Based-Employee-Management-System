package com.lankatech.ems.dao;

import com.lankatech.ems.model.PerformanceRecord;
import java.util.List;
import java.util.Optional;

public interface PerformanceDao {

    PerformanceRecord save(PerformanceRecord record);
    Optional<PerformanceRecord> findById(int performanceId);
    List<PerformanceRecord> findByEmployee(int employeeId);
    List<PerformanceRecord> findByDepartment(int departmentId);
    void update(PerformanceRecord record);
    void deleteById(int performanceId);
}
