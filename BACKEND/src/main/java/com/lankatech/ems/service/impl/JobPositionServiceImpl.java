package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.DepartmentDao;
import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.JobPositionDao;
import com.lankatech.ems.dto.request.CreateJobPositionRequest;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.Department;
import com.lankatech.ems.model.JobPosition;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.JobPositionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobPositionServiceImpl implements JobPositionService {

    private final JobPositionDao jobPositionDao;
    private final DepartmentDao departmentDao;
    private final EmployeeDao employeeDao;
    private final ActivityLogService activityLogService;

    public JobPositionServiceImpl(JobPositionDao jobPositionDao, DepartmentDao departmentDao, EmployeeDao employeeDao,
                                  ActivityLogService activityLogService) {
        this.jobPositionDao = jobPositionDao;
        this.departmentDao = departmentDao;
        this.employeeDao = employeeDao;
        this.activityLogService = activityLogService;
    }

    @Override
    public JobPosition create(CreateJobPositionRequest r, int actorUserId) {
        Department department = requireDepartment(r.getDepartmentId());
        if (!"ACTIVE".equals(department.getStatus())) {
            throw new IllegalArgumentException("Cannot add a position to inactive department '" + department.getName() + "'");
        }
        checkDuplicateTitle(r.getTitle(), r.getDepartmentId(), null);

        JobPosition position = new JobPosition();
        position.setTitle(r.getTitle().trim());
        position.setDepartmentId(r.getDepartmentId());
        position.setDescription(r.getDescription());

        JobPosition saved = jobPositionDao.save(position);
        activityLogService.log(actorUserId, "CREATE_POSITION",
                "Created position #" + saved.getPositionId() + " " + saved.getTitle());
        return saved;
    }

    @Override
    public List<JobPosition> findAll() {
        return jobPositionDao.findAll();
    }

    @Override
    public List<JobPosition> findByDepartment(int departmentId) {
        requireDepartment(departmentId);
        return jobPositionDao.findByDepartment(departmentId);
    }

    @Override
    public JobPosition findById(int positionId) {
        return jobPositionDao.findById(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Job position not found: " + positionId));
    }

    @Override
    public JobPosition update(int positionId, CreateJobPositionRequest r, int actorUserId) {
        JobPosition existing = findById(positionId);
        requireDepartment(r.getDepartmentId());
        checkDuplicateTitle(r.getTitle(), r.getDepartmentId(), positionId);

        // Moving a position to another department would leave its holders in a
        // position that doesn't belong to their department.
        if (existing.getDepartmentId() != r.getDepartmentId()) {
            int holders = employeeDao.countByPosition(positionId);
            if (holders > 0) {
                throw new IllegalStateException("Cannot move '" + existing.getTitle() + "' to another department: "
                        + holders + " employee(s) hold this position");
            }
        }

        existing.setTitle(r.getTitle().trim());
        existing.setDepartmentId(r.getDepartmentId());
        existing.setDescription(r.getDescription());

        jobPositionDao.update(existing);
        activityLogService.log(actorUserId, "UPDATE_POSITION", "Updated position #" + positionId);
        return findById(positionId);
    }

    // Fails with HTTP 409 (via AbstractJdbcDao) if employees still hold this position.
    @Override
    public void delete(int positionId, int actorUserId) {
        findById(positionId);
        jobPositionDao.deleteById(positionId);
        activityLogService.log(actorUserId, "DELETE_POSITION", "Deleted position #" + positionId);
    }

    private Department requireDepartment(int departmentId) {
        return departmentDao.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
    }

    private void checkDuplicateTitle(String title, int departmentId, Integer ignorePositionId) {
        String wanted = title.trim();
        for (JobPosition p : jobPositionDao.findByDepartment(departmentId)) {
            if (p.getTitle().equalsIgnoreCase(wanted) && (ignorePositionId == null || p.getPositionId() != ignorePositionId)) {
                throw new DuplicateRecordException("The department already has a position called '" + p.getTitle() + "'");
            }
        }
    }
}
