package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.ActivityLogDao;
import com.lankatech.ems.dao.impl.rowmapper.ActivityLogRowMapper;
import com.lankatech.ems.model.ActivityLog;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class ActivityLogDaoImpl extends AbstractJdbcDao<ActivityLog, Integer> implements ActivityLogDao {

    private final ActivityLogRowMapper mapper = new ActivityLogRowMapper();

    public ActivityLogDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public ActivityLog save(ActivityLog log) {
        String sql = "INSERT INTO activity_logs (user_id, action, details, ip_address) VALUES (?, ?, ?, ?)";
        int id = executeInsertReturnId(sql, log.getUserId(), log.getAction(), log.getDetails(), log.getIpAddress());
        log.setLogId(id);
        return log;
    }

    @Override
    public List<ActivityLog> findRecent(int limit) {
        return queryList("SELECT * FROM activity_logs ORDER BY log_id DESC LIMIT ?", mapper, limit);
    }

    @Override
    public List<ActivityLog> findByUser(int userId) {
        return queryList("SELECT * FROM activity_logs WHERE user_id = ? ORDER BY log_id DESC", mapper, userId);
    }

    @Override
    public List<ActivityLog> findByAction(String action) {
        return queryList("SELECT * FROM activity_logs WHERE action = ? ORDER BY log_id DESC", mapper, action);
    }
}
