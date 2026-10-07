package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.CandidateApplicationDao;
import com.lankatech.ems.dao.DepartmentDao;
import com.lankatech.ems.dao.VacancyDao;
import com.lankatech.ems.dto.request.CreateVacancyRequest;
import com.lankatech.ems.enums.VacancyStatus;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.InvalidStatusTransitionException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.Department;
import com.lankatech.ems.model.Vacancy;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.VacancyService;
import com.lankatech.ems.util.EnumUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class VacancyServiceImpl implements VacancyService {

    private final VacancyDao vacancyDao;
    private final DepartmentDao departmentDao;
    private final CandidateApplicationDao applicationDao;
    private final ActivityLogService activityLogService;

    public VacancyServiceImpl(VacancyDao vacancyDao, DepartmentDao departmentDao,
                              CandidateApplicationDao applicationDao, ActivityLogService activityLogService) {
        this.vacancyDao = vacancyDao;
        this.departmentDao = departmentDao;
        this.applicationDao = applicationDao;
        this.activityLogService = activityLogService;
    }

    @Override
    public Vacancy create(CreateVacancyRequest r, int userId) {
        validate(r);
        checkDuplicateOpen(r, null);

        Vacancy vacancy = new Vacancy();
        vacancy.setTitle(r.getTitle().trim());
        vacancy.setDepartmentId(r.getDepartmentId());
        vacancy.setRequirements(r.getRequirements());
        vacancy.setDeadline(r.getDeadline());
        vacancy.setStatus(VacancyStatus.OPEN);
        vacancy.setCreatedBy(userId);

        Vacancy saved = vacancyDao.save(vacancy);
        activityLogService.log(userId, "POST_VACANCY", "Posted vacancy #" + saved.getVacancyId() + " " + saved.getTitle());
        return findById(saved.getVacancyId());
    }

    @Override
    public List<Vacancy> findAll(String status) {
        if (status == null || status.isBlank()) {
            return vacancyDao.findAll();
        }
        return vacancyDao.findByStatus(EnumUtils.parse(VacancyStatus.class, status, "status").name());
    }

    @Override
    public Vacancy findById(int vacancyId) {
        return vacancyDao.findById(vacancyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found: " + vacancyId));
    }

    @Override
    public Vacancy update(int vacancyId, CreateVacancyRequest r, int userId) {
        Vacancy existing = findById(vacancyId);
        if (existing.getStatus() != VacancyStatus.OPEN) {
            throw new InvalidStatusTransitionException("Only OPEN vacancies can be edited, current status: " + existing.getStatus());
        }
        validate(r);
        checkDuplicateOpen(r, vacancyId);

        existing.setTitle(r.getTitle().trim());
        existing.setDepartmentId(r.getDepartmentId());
        existing.setRequirements(r.getRequirements());
        existing.setDeadline(r.getDeadline());

        vacancyDao.update(existing);
        activityLogService.log(userId, "UPDATE_VACANCY", "Updated vacancy #" + vacancyId);
        return findById(vacancyId);
    }

    /**
     * Allowed transitions:
     *   OPEN   -> CLOSED or FILLED
     *   CLOSED -> OPEN (re-open) or FILLED
     *   FILLED -> (final)
     */
    @Override
    public Vacancy changeStatus(int vacancyId, String status, int userId) {
        Vacancy existing = findById(vacancyId);
        VacancyStatus current = existing.getStatus();
        VacancyStatus next = EnumUtils.parse(VacancyStatus.class, status, "status");

        if (current == next) {
            throw new InvalidStatusTransitionException("Vacancy #" + vacancyId + " is already " + current);
        }
        if (current == VacancyStatus.FILLED) {
            throw new InvalidStatusTransitionException("A FILLED vacancy cannot change status");
        }
        if (next == VacancyStatus.OPEN && existing.getDeadline() != null && existing.getDeadline().isBefore(LocalDate.now())) {
            throw new InvalidStatusTransitionException("The deadline (" + existing.getDeadline()
                    + ") has passed, so the vacancy can't be reopened. Post a new vacancy instead.");
        }

        vacancyDao.updateStatus(vacancyId, next.name());
        activityLogService.log(userId, "VACANCY_" + next.name(), "Vacancy #" + vacancyId + " changed from " + current + " to " + next);
        return findById(vacancyId);
    }

    @Override
    public Vacancy close(int vacancyId, int userId) {
        return changeStatus(vacancyId, VacancyStatus.CLOSED.name(), userId);
    }

    @Override
    public void delete(int vacancyId, int userId) {
        findById(vacancyId);
        int applications = applicationDao.findByVacancy(vacancyId).size();
        if (applications > 0) {
            throw new IllegalStateException("Vacancy #" + vacancyId + " has " + applications
                    + " application(s) and cannot be deleted. Close it instead.");
        }
        vacancyDao.deleteById(vacancyId);
        activityLogService.log(userId, "DELETE_VACANCY", "Deleted vacancy #" + vacancyId);
    }

    // Business rule: only one OPEN vacancy with the same title per department
    private void checkDuplicateOpen(CreateVacancyRequest r, Integer ignoreVacancyId) {
        String title = r.getTitle().trim();
        for (Vacancy v : vacancyDao.findByStatus(VacancyStatus.OPEN.name())) {
            boolean same = v.getDepartmentId() == r.getDepartmentId() && v.getTitle().equalsIgnoreCase(title);
            if (same && (ignoreVacancyId == null || v.getVacancyId() != ignoreVacancyId)) {
                throw new DuplicateRecordException("An open vacancy '" + v.getTitle() + "' already exists for this department (#"
                        + v.getVacancyId() + ")");
            }
        }
    }

    private void validate(CreateVacancyRequest r) {
        Department department = departmentDao.findById(r.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + r.getDepartmentId()));
        if (!"ACTIVE".equals(department.getStatus())) {
            throw new IllegalArgumentException("Cannot post a vacancy for inactive department '" + department.getName() + "'");
        }
        if (r.getDeadline() != null && r.getDeadline().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Deadline cannot be in the past");
        }
    }
}
