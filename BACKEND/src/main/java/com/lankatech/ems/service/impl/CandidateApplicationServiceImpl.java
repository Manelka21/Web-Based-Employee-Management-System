package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.CandidateApplicationDao;
import com.lankatech.ems.dao.VacancyDao;
import com.lankatech.ems.dto.request.CreateApplicationRequest;
import com.lankatech.ems.dto.request.UpdateApplicationRequest;
import com.lankatech.ems.dto.response.ApplicationStatusResponse;
import com.lankatech.ems.enums.ApplicationStatus;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.VacancyStatus;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.InvalidStatusTransitionException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.CandidateApplication;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.model.Vacancy;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.CandidateApplicationService;
import com.lankatech.ems.service.EmployeeService;
import com.lankatech.ems.service.NotificationService;
import com.lankatech.ems.util.EnumUtils;
import com.lankatech.ems.util.ValidationRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class CandidateApplicationServiceImpl implements CandidateApplicationService {

    private final CandidateApplicationDao applicationDao;
    private final VacancyDao vacancyDao;
    private final EmployeeService employeeService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public CandidateApplicationServiceImpl(CandidateApplicationDao applicationDao, VacancyDao vacancyDao,
                                           EmployeeService employeeService, NotificationService notificationService,
                                           ActivityLogService activityLogService) {
        this.applicationDao = applicationDao;
        this.vacancyDao = vacancyDao;
        this.employeeService = employeeService;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    @Override
    public CandidateApplication create(CreateApplicationRequest r, int userId) {
        Vacancy vacancy = requireVacancy(r.getVacancyId());

        if (vacancy.getStatus() != VacancyStatus.OPEN) {
            throw new IllegalArgumentException("Vacancy #" + vacancy.getVacancyId() + " is " + vacancy.getStatus()
                    + " and is not accepting applications");
        }

        LocalDate appliedDate = r.getAppliedDate() != null ? r.getAppliedDate() : LocalDate.now();
        if (appliedDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("The application date cannot be in the future");
        }
        if (vacancy.getDeadline() != null && appliedDate.isAfter(vacancy.getDeadline())) {
            throw new IllegalArgumentException("The application deadline (" + vacancy.getDeadline() + ") has passed");
        }

        String email = ValidationRules.normalizeEmail(r.getCandidateEmail());
        String nic = r.getCandidateNic() == null || r.getCandidateNic().isBlank() ? null : ValidationRules.normalizeNic(r.getCandidateNic());
        for (CandidateApplication existing : applicationDao.findByVacancy(vacancy.getVacancyId())) {
            if (existing.getStatus() == ApplicationStatus.WITHDRAWN) {
                continue;
            }
            if (existing.getCandidateEmail().equalsIgnoreCase(email)) {
                throw new DuplicateRecordException(email + " has already applied for this vacancy (application #"
                        + existing.getApplicationId() + ")");
            }
            if (nic != null && nic.equalsIgnoreCase(existing.getCandidateNic())) {
                throw new DuplicateRecordException("A candidate with NIC " + nic + " has already applied for this vacancy (application #"
                        + existing.getApplicationId() + ")");
            }
        }

        CandidateApplication app = new CandidateApplication();
        app.setVacancyId(vacancy.getVacancyId());
        app.setCandidateName(r.getCandidateName().trim());
        app.setCandidateEmail(email);
        app.setCandidatePhone(ValidationRules.normalizePhone(r.getCandidatePhone()));
        app.setCandidateNic(nic);
        app.setResumeNotes(r.getResumeNotes());
        app.setStatus(ApplicationStatus.APPLIED);
        app.setAppliedDate(appliedDate);

        CandidateApplication saved = applicationDao.save(app);
        activityLogService.log(userId, "REGISTER_APPLICATION",
                "Registered application #" + saved.getApplicationId() + " from " + saved.getCandidateName()
                        + " for vacancy #" + vacancy.getVacancyId());
        return findById(saved.getApplicationId());
    }

    @Override
    public List<CandidateApplication> findAll() {
        return applicationDao.findAll();
    }

    @Override
    public CandidateApplication findById(int applicationId) {
        return applicationDao.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
    }

    @Override
    public List<CandidateApplication> findByVacancy(int vacancyId) {
        requireVacancy(vacancyId);
        return applicationDao.findByVacancy(vacancyId);
    }

    /**
     * Moves an application through the pipeline.
     * HIRED, REJECTED and WITHDRAWN are final.
     *
     * @Transactional: hiring updates the application AND inserts an employee,
     * a notification and log entries. If any step fails, everything rolls back,
     * so we never have a HIRED candidate without an employee record.
     */
    @Override
    @Transactional
    public ApplicationStatusResponse updateStatus(int applicationId, String status, int userId) {
        CandidateApplication app = findById(applicationId);
        ApplicationStatus next = EnumUtils.parse(ApplicationStatus.class, status, "status");
        checkTransition(app.getStatus(), next);

        Employee createdEmployee = null;
        if (next == ApplicationStatus.HIRED) {
            createdEmployee = hire(app, userId);
        }

        applicationDao.updateStatus(applicationId, next.name());
        activityLogService.log(userId, "APPLICATION_" + next.name(),
                "Application #" + applicationId + " changed from " + app.getStatus() + " to " + next);

        return new ApplicationStatusResponse(findById(applicationId), createdEmployee);
    }

    @Override
    public CandidateApplication withdraw(int applicationId, int userId) {
        return updateStatus(applicationId, ApplicationStatus.WITHDRAWN.name(), userId).getApplication();
    }

    // Correct a candidate's details (e.g. add the NIC needed for hiring) while still in progress
    @Override
    public CandidateApplication update(int applicationId, UpdateApplicationRequest r, int userId) {
        CandidateApplication app = findById(applicationId);
        if (app.getStatus() == ApplicationStatus.HIRED || app.getStatus() == ApplicationStatus.REJECTED
                || app.getStatus() == ApplicationStatus.WITHDRAWN) {
            throw new InvalidStatusTransitionException("A " + app.getStatus() + " application can no longer be edited");
        }

        String email = ValidationRules.normalizeEmail(r.getCandidateEmail());
        String nic = r.getCandidateNic() == null || r.getCandidateNic().isBlank() ? null : ValidationRules.normalizeNic(r.getCandidateNic());
        for (CandidateApplication other : applicationDao.findByVacancy(app.getVacancyId())) {
            if (other.getApplicationId() == applicationId || other.getStatus() == ApplicationStatus.WITHDRAWN) {
                continue;
            }
            if (other.getCandidateEmail().equalsIgnoreCase(email)) {
                throw new DuplicateRecordException(email + " has already applied for this vacancy (application #" + other.getApplicationId() + ")");
            }
            if (nic != null && nic.equalsIgnoreCase(other.getCandidateNic())) {
                throw new DuplicateRecordException("NIC " + nic + " is already used by application #" + other.getApplicationId());
            }
        }

        app.setCandidateName(r.getCandidateName().trim());
        app.setCandidateEmail(email);
        app.setCandidatePhone(ValidationRules.normalizePhone(r.getCandidatePhone()));
        app.setCandidateNic(nic);
        app.setResumeNotes(r.getResumeNotes());
        applicationDao.updateCandidate(app);
        activityLogService.log(userId, "UPDATE_APPLICATION", "Updated candidate details on application #" + applicationId);
        return findById(applicationId);
    }

    private void checkTransition(ApplicationStatus current, ApplicationStatus next) {
        if (current == ApplicationStatus.HIRED || current == ApplicationStatus.REJECTED
                || current == ApplicationStatus.WITHDRAWN) {
            throw new InvalidStatusTransitionException("Application is already " + current + " and cannot be changed");
        }
        if (next == ApplicationStatus.APPLIED) {
            throw new InvalidStatusTransitionException("An application cannot be moved back to APPLIED");
        }
        if (next == current) {
            throw new InvalidStatusTransitionException("Application is already " + current);
        }
    }

    // Business rule («extend» Register New Employee): hiring creates an Employee record from the application.
    private Employee hire(CandidateApplication app, int userId) {
        if (app.getCandidateNic() == null || app.getCandidateNic().isBlank()) {
            throw new IllegalArgumentException("Candidate NIC is required before hiring (application #"
                    + app.getApplicationId() + ")");
        }

        Vacancy vacancy = requireVacancy(app.getVacancyId());
        if (vacancy.getStatus() == VacancyStatus.FILLED) {
            throw new InvalidStatusTransitionException("Vacancy '" + vacancy.getTitle() + "' is already FILLED. Reject the remaining candidates instead.");
        }

        Employee newEmployee = new Employee();
        newEmployee.setFirstName(extractFirstName(app.getCandidateName()));
        newEmployee.setLastName(extractLastName(app.getCandidateName()));
        newEmployee.setNic(app.getCandidateNic());
        newEmployee.setEmail(app.getCandidateEmail());
        newEmployee.setPhone(app.getCandidatePhone());
        newEmployee.setHireDate(LocalDate.now());
        newEmployee.setStatus(EmployeeStatus.PROBATION);
        newEmployee.setDepartmentId(vacancy.getDepartmentId());   // link to the vacancy's department

        Employee saved = employeeService.create(newEmployee, userId);

        activityLogService.log(userId, "HIRE_CANDIDATE",
                "Hired candidate #" + app.getApplicationId() + " — created Employee #" + saved.getEmployeeId());

        notificationService.send(vacancy.getCreatedBy(), "CANDIDATE_HIRED",
                app.getCandidateName() + " was hired for '" + vacancy.getTitle() + "' and added as employee #"
                        + saved.getEmployeeId() + " (status PROBATION)");
        return saved;
    }

    private String extractFirstName(String fullName) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    private String extractLastName(String fullName) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? "-" : trimmed.substring(space + 1).trim();
    }

    private Vacancy requireVacancy(int vacancyId) {
        return vacancyDao.findById(vacancyId)
                .orElseThrow(() -> new ResourceNotFoundException("Vacancy not found: " + vacancyId));
    }
}
