package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.RecordPerformanceRequest;
import com.lankatech.ems.model.PerformanceRecord;

import java.util.List;

public interface PerformanceService {

    PerformanceRecord create(RecordPerformanceRequest request, int userId);
    List<PerformanceRecord> findByEmployee(int employeeId, int userId);
    List<PerformanceRecord> findByTeam(int departmentId, int userId);
    List<PerformanceRecord> findMine(int userId);
    PerformanceRecord update(int performanceId, RecordPerformanceRequest request, int userId);
    void delete(int performanceId, int userId);
}
