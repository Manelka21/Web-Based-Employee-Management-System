package com.lankatech.ems.service;

import com.lankatech.ems.model.ActivityLog;

import java.util.List;

public interface ActivityLogService {

    void log(int userId, String action, String details);
    List<ActivityLog> findRecent(int limit);
    List<ActivityLog> findByUser(int userId);
    List<ActivityLog> findByAction(String action);
}
