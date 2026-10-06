package com.lankatech.ems.dao;

import com.lankatech.ems.model.ActivityLog;
import java.util.List;

public interface ActivityLogDao {

    ActivityLog save(ActivityLog log);
    List<ActivityLog> findRecent(int limit);
    List<ActivityLog> findByUser(int userId);
    List<ActivityLog> findByAction(String action);
}
