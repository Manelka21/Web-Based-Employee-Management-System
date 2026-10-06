package com.lankatech.ems.dao;

import com.lankatech.ems.model.AttendanceRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceDao {

    AttendanceRecord save(AttendanceRecord record);
    Optional<AttendanceRecord> findById(int attendanceId);
    Optional<AttendanceRecord> findByEmployeeAndDate(int employeeId, LocalDate date);
    List<AttendanceRecord> findByEmployee(int employeeId);
    List<AttendanceRecord> findByDepartment(int departmentId);
    List<AttendanceRecord> findByEmployeeAndPeriod(int employeeId, LocalDate start, LocalDate end);
    void update(AttendanceRecord record);
    void deleteById(int attendanceId);
    int countByDate(LocalDate date);
}
