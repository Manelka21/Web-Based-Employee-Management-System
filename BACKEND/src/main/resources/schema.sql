-- ============================================================
-- Employee Management System — MySQL schema
-- LankaTech Services (Pvt) Ltd
-- Matches the EER diagram from IT2140.
--
-- Run this ONCE in MySQL Workbench before starting the app.
-- WARNING: it drops and recreates the ems_db database.
-- ============================================================

DROP DATABASE IF EXISTS ems_db;
CREATE DATABASE ems_db;
USE ems_db;

-- ---------- USERS (flat table — all 6 roles share the same columns) ----------
CREATE TABLE users (
    user_id            INT AUTO_INCREMENT PRIMARY KEY,
    email              VARCHAR(120) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    first_name         VARCHAR(80)  NOT NULL,
    last_name          VARCHAR(80)  NOT NULL,
    phone_number       VARCHAR(20),
    role               VARCHAR(30)  NOT NULL,   -- EMPLOYEE / HR_MANAGER / DEPT_SUPERVISOR / PAYROLL_EXECUTIVE / COMPANY_DIRECTOR / IT_ADMIN
    employee_id        INT,                      -- nullable FK: set once a User is linked to an Employee record
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active                 BOOLEAN  NOT NULL DEFAULT TRUE,   -- FALSE = disabled by the IT Administrator
    must_change_password   BOOLEAN  NOT NULL DEFAULT FALSE,  -- TRUE after an admin password reset
    failed_login_attempts  INT      NOT NULL DEFAULT 0,
    locked_until           DATETIME                          -- set after 5 wrong passwords in a row
);

-- ---------- DEPARTMENTS ----------
CREATE TABLE departments (
    department_id       INT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(120) NOT NULL UNIQUE,
    description         VARCHAR(500),
    head_of_dept_id     INT,                      -- FK to employees (set after employees table exists)
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'  -- ACTIVE / INACTIVE
);

-- ---------- JOB POSITIONS ----------
CREATE TABLE job_positions (
    position_id         INT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(120) NOT NULL,
    department_id       INT NOT NULL,
    description         VARCHAR(500),
    CONSTRAINT fk_pos_dept FOREIGN KEY (department_id) REFERENCES departments(department_id)
);

-- ---------- EMPLOYEES (the managed employee record — separate from user account) ----------
CREATE TABLE employees (
    employee_id         INT AUTO_INCREMENT PRIMARY KEY,
    first_name          VARCHAR(80)  NOT NULL,
    last_name           VARCHAR(80)  NOT NULL,
    nic                 VARCHAR(20)  NOT NULL UNIQUE,
    email               VARCHAR(120) NOT NULL UNIQUE,
    phone               VARCHAR(20),
    address             VARCHAR(255),
    department_id       INT,
    position_id         INT,
    hire_date           DATE NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / INACTIVE / ON_LEAVE / PROBATION
    gender              VARCHAR(10),                              -- MALE / FEMALE / OTHER (needed for maternity leave)
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_emp_dept FOREIGN KEY (department_id) REFERENCES departments(department_id),
    CONSTRAINT fk_emp_pos  FOREIGN KEY (position_id)   REFERENCES job_positions(position_id),
    CONSTRAINT chk_emp_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'))
);

-- Now add the deferred FKs
ALTER TABLE users
    ADD CONSTRAINT fk_user_employee FOREIGN KEY (employee_id) REFERENCES employees(employee_id);

-- 1:1 rule: an employee record can be linked to at most one user account (NULLs are allowed many times)
ALTER TABLE users
    ADD CONSTRAINT uq_user_employee UNIQUE (employee_id);

ALTER TABLE departments
    ADD CONSTRAINT fk_dept_head FOREIGN KEY (head_of_dept_id) REFERENCES employees(employee_id);

-- ---------- VACANCIES (Recruitment & Onboarding) ----------
CREATE TABLE vacancies (
    vacancy_id          INT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(120) NOT NULL,
    department_id       INT NOT NULL,
    requirements        VARCHAR(1000),
    deadline            DATE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',  -- OPEN / CLOSED / FILLED
    created_by          INT NOT NULL,                           -- user_id of the HR Manager
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vac_dept    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    CONSTRAINT fk_vac_creator FOREIGN KEY (created_by)    REFERENCES users(user_id)
);

-- ---------- CANDIDATE APPLICATIONS (Recruitment & Onboarding) ----------
CREATE TABLE candidate_applications (
    application_id      INT AUTO_INCREMENT PRIMARY KEY,
    vacancy_id          INT NOT NULL,
    candidate_name      VARCHAR(120) NOT NULL,
    candidate_email     VARCHAR(120) NOT NULL,
    candidate_phone     VARCHAR(20),
    candidate_nic       VARCHAR(20),
    resume_notes        VARCHAR(1000),
    status              VARCHAR(30)  NOT NULL DEFAULT 'APPLIED',  -- APPLIED / SHORTLISTED / INTERVIEWED / HIRED / REJECTED / WITHDRAWN
    applied_date        DATE         NOT NULL,
    updated_at          DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_vacancy FOREIGN KEY (vacancy_id) REFERENCES vacancies(vacancy_id)
);

-- ---------- TRAINING PROGRAMS ----------
CREATE TABLE training_programs (
    program_id          INT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(120) NOT NULL,
    trainer             VARCHAR(120),
    start_date          DATE,
    end_date            DATE,
    department_id       INT,                                     -- null = company-wide
    capacity            INT,
    description         VARCHAR(500),
    status              VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',  -- SCHEDULED / IN_PROGRESS / COMPLETED / CANCELLED
    created_by          INT,
    CONSTRAINT fk_tp_dept    FOREIGN KEY (department_id) REFERENCES departments(department_id),
    CONSTRAINT fk_tp_creator FOREIGN KEY (created_by)    REFERENCES users(user_id),
    CONSTRAINT chk_tp_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT chk_tp_dates    CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
);

-- ---------- TRAINING ENROLLMENTS ----------
CREATE TABLE training_enrollments (
    enrollment_id       INT AUTO_INCREMENT PRIMARY KEY,
    employee_id         INT NOT NULL,
    program_id          INT NOT NULL,
    enrolled_date       DATE NOT NULL,
    completion_status   VARCHAR(20)  NOT NULL DEFAULT 'ENROLLED',  -- ENROLLED / IN_PROGRESS / COMPLETED / DROPPED
    completion_date     DATE,
    CONSTRAINT fk_te_emp  FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
    CONSTRAINT fk_te_prog FOREIGN KEY (program_id)  REFERENCES training_programs(program_id),
    CONSTRAINT uq_enrollment UNIQUE (employee_id, program_id)
);

-- ---------- ATTENDANCE RECORDS ----------
CREATE TABLE attendance_records (
    attendance_id       INT AUTO_INCREMENT PRIMARY KEY,
    employee_id         INT NOT NULL,
    date                DATE NOT NULL,
    check_in_time       TIME,
    check_out_time      TIME,
    status              VARCHAR(20)  NOT NULL DEFAULT 'PRESENT',   -- PRESENT / ABSENT / LATE / HALF_DAY
    override_reason     VARCHAR(255),
    CONSTRAINT fk_att_emp FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
    CONSTRAINT uq_attendance UNIQUE (employee_id, date),
    CONSTRAINT chk_att_times CHECK (check_in_time IS NULL OR check_out_time IS NULL OR check_out_time > check_in_time)
);

-- ---------- LEAVE REQUESTS ----------
CREATE TABLE leave_requests (
    leave_id            INT AUTO_INCREMENT PRIMARY KEY,
    employee_id         INT NOT NULL,
    leave_type          VARCHAR(30)  NOT NULL,                     -- ANNUAL / SICK / CASUAL / MATERNITY / OTHER
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    reason              VARCHAR(500),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',   -- PENDING / APPROVED / REJECTED / CANCELLED
    approved_by         INT,                                       -- user_id of the approver (Supervisor or HR)
    approved_date       DATETIME,
    submitted_date      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_lr_emp      FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
    CONSTRAINT fk_lr_approver FOREIGN KEY (approved_by) REFERENCES users(user_id),
    CONSTRAINT chk_lr_dates   CHECK (end_date >= start_date)
);

-- ---------- PAYROLL RECORDS ----------
CREATE TABLE payroll_records (
    payroll_id          INT AUTO_INCREMENT PRIMARY KEY,
    employee_id         INT NOT NULL,
    pay_period_start    DATE NOT NULL,
    pay_period_end      DATE NOT NULL,
    base_salary         DECIMAL(12,2) NOT NULL,
    overtime_hours      DECIMAL(5,2)  DEFAULT 0,
    overtime_amount     DECIMAL(12,2) DEFAULT 0,
    deductions          DECIMAL(12,2) DEFAULT 0,
    net_pay             DECIMAL(12,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL DEFAULT 'DRAFT',    -- DRAFT / FINALIZED / PAID / VOIDED
    generated_by        INT,
    generated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pr_emp       FOREIGN KEY (employee_id)  REFERENCES employees(employee_id),
    CONSTRAINT fk_pr_generator FOREIGN KEY (generated_by) REFERENCES users(user_id),
    CONSTRAINT chk_pr_period   CHECK (pay_period_end >= pay_period_start),
    CONSTRAINT chk_pr_amounts  CHECK (base_salary > 0 AND overtime_hours >= 0 AND deductions >= 0 AND net_pay >= 0)
);

-- ---------- PERFORMANCE RECORDS ----------
CREATE TABLE performance_records (
    performance_id      INT AUTO_INCREMENT PRIMARY KEY,
    employee_id         INT NOT NULL,
    supervisor_id       INT NOT NULL,                              -- user_id of the Department Supervisor
    feedback            VARCHAR(1000) NOT NULL,
    rating              INT,                                       -- 1–5 scale, nullable
    review_date         DATE NOT NULL,
    created_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_perf_emp  FOREIGN KEY (employee_id)  REFERENCES employees(employee_id),
    CONSTRAINT fk_perf_sup  FOREIGN KEY (supervisor_id) REFERENCES users(user_id),
    CONSTRAINT chk_perf_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5)
);

-- ---------- NOTIFICATIONS ----------
CREATE TABLE notifications (
    notification_id     INT AUTO_INCREMENT PRIMARY KEY,
    user_id             INT NOT NULL,
    type                VARCHAR(50),
    message             VARCHAR(500),
    sent_at             DATETIME     DEFAULT CURRENT_TIMESTAMP,
    channel             VARCHAR(20)  DEFAULT 'WEBSITE',            -- EMAIL / SMS / WEBSITE
    read_status         BOOLEAN      DEFAULT FALSE,
    delivery_status     VARCHAR(20)  NOT NULL DEFAULT 'SENT',      -- SENT (in-app) / PENDING / SENT / FAILED (email outbox)
    delivered_at        DATETIME,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ---------- PUBLIC HOLIDAYS (not counted as leave days) ----------
CREATE TABLE public_holidays (
    holiday_date        DATE         PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    created_by          INT,
    CONSTRAINT fk_hol_creator FOREIGN KEY (created_by) REFERENCES users(user_id)
);

-- ---------- SALARIES ON FILE (kept apart from employees so supervisors never see pay) ----------
CREATE TABLE employee_salaries (
    employee_id         INT          PRIMARY KEY,
    base_salary         DECIMAL(12,2) NOT NULL,
    updated_by          INT,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sal_emp     FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
    CONSTRAINT fk_sal_updater FOREIGN KEY (updated_by)  REFERENCES users(user_id),
    CONSTRAINT chk_sal_amount CHECK (base_salary > 0)
);

-- ---------- ACTIVITY LOG (audit trail for IT Admin) ----------
CREATE TABLE activity_logs (
    log_id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id             INT NOT NULL,
    action              VARCHAR(120) NOT NULL,
    details             VARCHAR(500),
    ip_address          VARCHAR(45),
    timestamp           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);


-- ============================================================
-- Seed data
--
-- The password for ALL seed users is: password123
--
-- The users are inserted with the placeholder 'CHANGE_ME_ON_STARTUP'.
-- When the Spring Boot app starts, DataInitializer replaces every
-- placeholder with a real BCrypt hash of "password123".
-- So you never have to generate a hash by hand.
-- ============================================================

INSERT INTO departments (name, description)
VALUES ('Human Resources', 'HR department — manages recruitment, onboarding, employee records, and compliance');

INSERT INTO departments (name, description)
VALUES ('Engineering', 'Software development and IT operations');

INSERT INTO job_positions (title, department_id, description)
VALUES ('HR Manager', 1, 'Manages all HR operations');

INSERT INTO job_positions (title, department_id, description)
VALUES ('Software Engineer', 2, 'Develops and maintains software applications');

INSERT INTO job_positions (title, department_id, description)
VALUES ('Engineering Supervisor', 2, 'Supervises the engineering team');

-- Managed employee records
-- Employee #1: a regular engineer (has the EMPLOYEE login below)
INSERT INTO employees (first_name, last_name, nic, email, phone, address, department_id, position_id, hire_date, status, gender)
VALUES ('Tharindu', 'Silva', '199512345678', 'employee@lankatech.lk', '0711111111', 'Colombo 05', 2, 2, '2024-01-15', 'ACTIVE', 'MALE');

-- Employee #2: the Engineering supervisor (has the DEPT_SUPERVISOR login below)
INSERT INTO employees (first_name, last_name, nic, email, phone, address, department_id, position_id, hire_date, status, gender)
VALUES ('Kasun', 'Wickramasinghe', '198812345678', 'supervisor@lankatech.lk', '0722222222', 'Kandy', 2, 3, '2020-03-01', 'ACTIVE', 'MALE');

UPDATE departments SET head_of_dept_id = 2 WHERE department_id = 2;

-- User accounts (user_id 1..6)
INSERT INTO users (email, password_hash, first_name, last_name, role)
VALUES ('hr@lankatech.lk', 'CHANGE_ME_ON_STARTUP', 'Nadeesha', 'Perera', 'HR_MANAGER');

INSERT INTO users (email, password_hash, first_name, last_name, role)
VALUES ('admin@lankatech.lk', 'CHANGE_ME_ON_STARTUP', 'Ruwan', 'Jayasinghe', 'IT_ADMIN');

INSERT INTO users (email, password_hash, first_name, last_name, role)
VALUES ('director@lankatech.lk', 'CHANGE_ME_ON_STARTUP', 'Sanjaya', 'Fernando', 'COMPANY_DIRECTOR');

INSERT INTO users (email, password_hash, first_name, last_name, role)
VALUES ('payroll@lankatech.lk', 'CHANGE_ME_ON_STARTUP', 'Dilini', 'Karunaratne', 'PAYROLL_EXECUTIVE');

INSERT INTO users (email, password_hash, first_name, last_name, role, employee_id)
VALUES ('supervisor@lankatech.lk', 'CHANGE_ME_ON_STARTUP', 'Kasun', 'Wickramasinghe', 'DEPT_SUPERVISOR', 2);

INSERT INTO users (email, password_hash, first_name, last_name, role, employee_id)
VALUES ('employee@lankatech.lk', 'CHANGE_ME_ON_STARTUP', 'Tharindu', 'Silva', 'EMPLOYEE', 1);
