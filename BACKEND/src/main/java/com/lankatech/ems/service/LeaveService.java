package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.ApplyLeaveRequest;
import com.lankatech.ems.model.LeaveRequest;

import java.util.List;
import java.util.Map;

public interface LeaveService {

    LeaveRequest apply(ApplyLeaveRequest request, int userId);
    List<LeaveRequest> findMine(int userId);
    List<LeaveRequest> findByEmployee(int employeeId, int userId);
    List<LeaveRequest> findPending(int userId);
    LeaveRequest findById(int leaveId, int userId);
    LeaveRequest approve(int leaveId, int userId, String comment);
    LeaveRequest reject(int leaveId, int userId, String comment);
    LeaveRequest cancel(int leaveId, int userId);
    List<Map<String, Object>> balance(int userId);
}
