package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.ActivityLogDao;
import com.lankatech.ems.model.ActivityLog;
import com.lankatech.ems.service.ActivityLogService;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private static final int MAX_DETAILS_LENGTH = 500;

    private final ActivityLogDao activityLogDao;

    public ActivityLogServiceImpl(ActivityLogDao activityLogDao) {
        this.activityLogDao = activityLogDao;
    }

    @Override
    public void log(int userId, String action, String details) {
        ActivityLog entry = new ActivityLog(userId, action, truncate(details));
        entry.setIpAddress(currentIpAddress());
        activityLogDao.save(entry);
    }

    @Override
    public List<ActivityLog> findRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        return activityLogDao.findRecent(safeLimit);
    }

    @Override
    public List<ActivityLog> findByUser(int userId) {
        return activityLogDao.findByUser(userId);
    }

    @Override
    public List<ActivityLog> findByAction(String action) {
        return activityLogDao.findByAction(action.toUpperCase());
    }

    // IP address of the HTTP request currently being handled (null outside a request, e.g. at startup)
    private String currentIpAddress() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest().getRemoteAddr();
        }
        return null;
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_DETAILS_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_DETAILS_LENGTH);
    }
}
