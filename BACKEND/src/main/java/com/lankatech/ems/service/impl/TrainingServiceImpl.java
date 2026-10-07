package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.DepartmentDao;
import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.TrainingEnrollmentDao;
import com.lankatech.ems.dao.TrainingProgramDao;
import com.lankatech.ems.dto.request.CreateTrainingProgramRequest;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.EnrollmentStatus;
import com.lankatech.ems.enums.ProgramStatus;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.InvalidStatusTransitionException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.model.TrainingEnrollment;
import com.lankatech.ems.model.TrainingProgram;
import com.lankatech.ems.security.AccessGuard;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.NotificationService;
import com.lankatech.ems.service.TrainingService;
import com.lankatech.ems.util.EnumUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TrainingServiceImpl implements TrainingService {

    private final TrainingProgramDao programDao;
    private final TrainingEnrollmentDao enrollmentDao;
    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public TrainingServiceImpl(TrainingProgramDao programDao, TrainingEnrollmentDao enrollmentDao,
                               EmployeeDao employeeDao, DepartmentDao departmentDao, AccessGuard accessGuard,
                               NotificationService notificationService, ActivityLogService activityLogService) {
        this.programDao = programDao;
        this.enrollmentDao = enrollmentDao;
        this.employeeDao = employeeDao;
        this.departmentDao = departmentDao;
        this.accessGuard = accessGuard;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    // ======================= PROGRAMS =======================

    @Override
    public TrainingProgram createProgram(CreateTrainingProgramRequest r, int userId) {
        validateProgram(r);
        if (r.getStartDate() != null && r.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A new program cannot start in the past");
        }

        TrainingProgram program = new TrainingProgram();
        applyProgramFields(program, r);
        program.setStatus(ProgramStatus.SCHEDULED);
        program.setCreatedBy(userId);

        TrainingProgram saved = programDao.save(program);
        activityLogService.log(userId, "CREATE_TRAINING_PROGRAM",
                "Created training program #" + saved.getProgramId() + " " + saved.getTitle());
        return findProgram(saved.getProgramId());
    }

    @Override
    public List<TrainingProgram> findAllPrograms() {
        return programDao.findAll();
    }

    @Override
    public TrainingProgram findProgram(int programId) {
        return programDao.findById(programId)
                .orElseThrow(() -> new ResourceNotFoundException("Training program not found: " + programId));
    }

    @Override
    public TrainingProgram updateProgram(int programId, CreateTrainingProgramRequest r, int userId) {
        TrainingProgram existing = findProgram(programId);
        if (existing.getStatus() == ProgramStatus.CANCELLED || existing.getStatus() == ProgramStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("A " + existing.getStatus() + " program cannot be edited");
        }
        validateProgram(r);

        int activeEnrollments = enrollmentDao.countActiveByProgram(programId);
        if (r.getCapacity() != null && r.getCapacity() < activeEnrollments) {
            throw new IllegalArgumentException("Capacity cannot be lower than the " + activeEnrollments + " current enrollment(s)");
        }

        applyProgramFields(existing, r);
        if (r.getStatus() != null && !r.getStatus().isBlank()) {
            ProgramStatus status = EnumUtils.parse(ProgramStatus.class, r.getStatus(), "status");
            // Cancelling must go through PATCH /cancel so enrolled employees are notified
            if (status == ProgramStatus.CANCELLED && existing.getStatus() != ProgramStatus.CANCELLED) {
                throw new InvalidStatusTransitionException("Use PATCH /api/training-programs/" + programId + "/cancel to cancel a program");
            }
            existing.setStatus(status);
        }

        programDao.update(existing);
        activityLogService.log(userId, "UPDATE_TRAINING_PROGRAM", "Updated training program #" + programId);
        return findProgram(programId);
    }

    @Override
    public TrainingProgram cancelProgram(int programId, int userId) {
        TrainingProgram existing = findProgram(programId);
        if (existing.getStatus() == ProgramStatus.CANCELLED || existing.getStatus() == ProgramStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("Program is already " + existing.getStatus());
        }
        programDao.updateStatus(programId, ProgramStatus.CANCELLED.name());

        // Tell everyone who was enrolled
        for (TrainingEnrollment enrollment : enrollmentDao.findByProgram(programId)) {
            if (enrollment.getCompletionStatus() != EnrollmentStatus.DROPPED) {
                notificationService.sendToEmployee(enrollment.getEmployeeId(), "TRAINING_CANCELLED",
                        "The training program '" + existing.getTitle() + "' has been cancelled");
            }
        }

        activityLogService.log(userId, "CANCEL_TRAINING_PROGRAM", "Cancelled training program #" + programId);
        return findProgram(programId);
    }

    @Override
    public void deleteProgram(int programId, int userId) {
        findProgram(programId);
        if (!enrollmentDao.findByProgram(programId).isEmpty()) {
            throw new IllegalStateException("Program #" + programId + " has enrollments and cannot be deleted. Cancel it instead.");
        }
        programDao.deleteById(programId);
        activityLogService.log(userId, "DELETE_TRAINING_PROGRAM", "Deleted training program #" + programId);
    }

    // ======================= ENROLLMENTS =======================

    @Override
    public TrainingEnrollment enrol(int employeeId, int programId, int userId) {
        // Supervisors may only enrol members of their own team
        accessGuard.checkEmployeeScope(userId, employeeId);
        return doEnrol(employeeId, programId, userId);
    }

    @Override
    public TrainingEnrollment selfEnrol(int programId, int userId) {
        int employeeId = accessGuard.requireEmployeeId(userId);
        return doEnrol(employeeId, programId, userId);
    }

    @Override
    public List<TrainingEnrollment> findByEmployee(int employeeId, int userId) {
        accessGuard.checkEmployeeScope(userId, employeeId);
        return enrollmentDao.findByEmployee(employeeId);
    }

    @Override
    public List<TrainingEnrollment> findMine(int userId) {
        return enrollmentDao.findByEmployee(accessGuard.requireEmployeeId(userId));
    }

    @Override
    public List<TrainingEnrollment> findByProgram(int programId) {
        findProgram(programId);
        return enrollmentDao.findByProgram(programId);
    }

    @Override
    public TrainingEnrollment complete(int enrollmentId, int userId) {
        TrainingEnrollment enrollment = requireEnrollment(enrollmentId);
        accessGuard.checkEmployeeScope(userId, enrollment.getEmployeeId());
        accessGuard.checkNotSelf(userId, enrollment.getEmployeeId(), "mark training as completed");

        if (enrollment.getCompletionStatus() == EnrollmentStatus.COMPLETED
                || enrollment.getCompletionStatus() == EnrollmentStatus.DROPPED) {
            throw new InvalidStatusTransitionException("Enrollment is already " + enrollment.getCompletionStatus());
        }

        // Business rule: training can't be completed before it has started
        TrainingProgram program = findProgram(enrollment.getProgramId());
        if (program.getStartDate() != null && program.getStartDate().isAfter(LocalDate.now())) {
            throw new InvalidStatusTransitionException("'" + program.getTitle() + "' only starts on " + program.getStartDate()
                    + ", so it can't be completed yet");
        }
        if (program.getStatus() == ProgramStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("'" + program.getTitle() + "' was cancelled");
        }

        enrollmentDao.updateStatus(enrollmentId, EnrollmentStatus.COMPLETED.name(), LocalDate.now());
        notificationService.sendToEmployee(enrollment.getEmployeeId(), "TRAINING_COMPLETED",
                "Congratulations! You completed the training program '" + program.getTitle() + "'");
        activityLogService.log(userId, "COMPLETE_TRAINING",
                "Marked enrollment #" + enrollmentId + " (employee #" + enrollment.getEmployeeId() + ") as COMPLETED");
        return requireEnrollment(enrollmentId);
    }

    @Override
    public TrainingEnrollment drop(int enrollmentId, int userId) {
        TrainingEnrollment enrollment = requireEnrollment(enrollmentId);
        accessGuard.checkEmployeeScope(userId, enrollment.getEmployeeId());

        if (enrollment.getCompletionStatus() == EnrollmentStatus.COMPLETED
                || enrollment.getCompletionStatus() == EnrollmentStatus.DROPPED) {
            throw new InvalidStatusTransitionException("Enrollment is already " + enrollment.getCompletionStatus());
        }

        enrollmentDao.updateStatus(enrollmentId, EnrollmentStatus.DROPPED.name(), null);
        activityLogService.log(userId, "DROP_TRAINING",
                "Dropped enrollment #" + enrollmentId + " (employee #" + enrollment.getEmployeeId() + ")");
        return requireEnrollment(enrollmentId);
    }

    // ======================= helpers =======================

    private TrainingEnrollment doEnrol(int employeeId, int programId, int userId) {
        Employee employee = employeeDao.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        TrainingProgram program = findProgram(programId);

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new IllegalArgumentException("Inactive employees cannot be enrolled in training");
        }
        if (program.getStatus() != ProgramStatus.SCHEDULED && program.getStatus() != ProgramStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Program '" + program.getTitle() + "' is " + program.getStatus()
                    + " and not open for enrollment");
        }
        if (program.getEndDate() != null && program.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Program '" + program.getTitle() + "' ended on " + program.getEndDate());
        }
        // Department-specific programs are only for that department's employees
        if (program.getDepartmentId() != null && !program.getDepartmentId().equals(employee.getDepartmentId())) {
            throw new IllegalArgumentException("Program '" + program.getTitle() + "' is only for department #"
                    + program.getDepartmentId());
        }

        Optional<TrainingEnrollment> existing = enrollmentDao.findByEmployeeAndProgram(employeeId, programId);
        if (existing.isPresent() && existing.get().getCompletionStatus() != EnrollmentStatus.DROPPED) {
            throw new DuplicateRecordException("Employee #" + employeeId + " is already enrolled in program #" + programId);
        }

        // Capacity check
        if (program.getCapacity() != null) {
            int currentCount = enrollmentDao.countActiveByProgram(programId);
            if (currentCount >= program.getCapacity()) {
                throw new IllegalArgumentException("Training program is full — capacity: " + program.getCapacity());
            }
        }

        TrainingEnrollment result;
        if (existing.isPresent()) {
            // Previously dropped: re-activate the same row (the table has a UNIQUE(employee_id, program_id))
            enrollmentDao.reEnroll(existing.get().getEnrollmentId(), LocalDate.now());
            result = requireEnrollment(existing.get().getEnrollmentId());
        } else {
            TrainingEnrollment enrollment = new TrainingEnrollment();
            enrollment.setEmployeeId(employeeId);
            enrollment.setProgramId(programId);
            enrollment.setEnrolledDate(LocalDate.now());
            enrollment.setCompletionStatus(EnrollmentStatus.ENROLLED);
            result = enrollmentDao.save(enrollment);
        }

        notificationService.sendToEmployee(employeeId, "TRAINING_ENROLLED",
                "You have been enrolled in '" + program.getTitle() + "'"
                        + (program.getStartDate() != null ? " starting " + program.getStartDate() : ""));
        activityLogService.log(userId, "ENROLL_TRAINING",
                "Enrolled employee #" + employeeId + " in training program #" + programId);
        return result;
    }

    private void validateProgram(CreateTrainingProgramRequest r) {
        if (r.getStartDate() != null && r.getEndDate() != null && r.getEndDate().isBefore(r.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        if (r.getDepartmentId() != null && departmentDao.findById(r.getDepartmentId()).isEmpty()) {
            throw new ResourceNotFoundException("Department not found: " + r.getDepartmentId());
        }
    }

    private void applyProgramFields(TrainingProgram program, CreateTrainingProgramRequest r) {
        program.setTitle(r.getTitle().trim());
        program.setTrainer(r.getTrainer());
        program.setStartDate(r.getStartDate());
        program.setEndDate(r.getEndDate());
        program.setDepartmentId(r.getDepartmentId());
        program.setCapacity(r.getCapacity());
        program.setDescription(r.getDescription());
    }

    private TrainingEnrollment requireEnrollment(int enrollmentId) {
        return enrollmentDao.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + enrollmentId));
    }
}
