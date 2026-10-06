package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.LogAttendanceRequest;
import com.lankatech.ems.model.AttendanceRecord;

import java.util.List;

public interface AttendanceService {

    AttendanceRecord log(LogAttendanceRequest request, int userId);
    AttendanceRecord checkIn(int userId);
    AttendanceRecord checkOut(int userId);
    List<AttendanceRecord> findByEmployee(int employeeId, int userId);
    List<AttendanceRecord> findMine(int userId);
    List<AttendanceRecord> findByDepartment(int departmentId, int userId);
    AttendanceRecord correct(int attendanceId, LogAttendanceRequest request, int userId);
    void delete(int attendanceId, int userId);
}
