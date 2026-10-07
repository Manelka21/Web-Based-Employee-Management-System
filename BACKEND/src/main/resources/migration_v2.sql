-- ============================================================
-- migration_v2.sql — upgrade an EXISTING ems_db without losing data
--
-- Use this instead of re-running schema.sql (which drops the database).
-- Run it ONCE on a database created by the original schema.sql, then run
-- seed.sql (safe to re-run) to fill in genders, holidays and salaries.
--
-- If a CHECK constraint fails, some existing rows break that rule
-- (e.g. a leave request whose end date is before its start date).
-- Fix those rows, then run the remaining statements.
-- ============================================================

USE ems_db;

-- ---------- QA round 1 ----------
ALTER TABLE users ADD CONSTRAINT uq_user_employee UNIQUE (employee_id);

ALTER TABLE training_programs
    ADD CONSTRAINT chk_tp_capacity CHECK (capacity IS NULL OR capacity > 0),
    ADD CONSTRAINT chk_tp_dates    CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date);

ALTER TABLE attendance_records
    ADD CONSTRAINT chk_att_times CHECK (check_in_time IS NULL OR check_out_time IS NULL OR check_out_time > check_in_time);

ALTER TABLE leave_requests
    ADD CONSTRAINT chk_lr_dates CHECK (end_date >= start_date);

ALTER TABLE payroll_records
    ADD CONSTRAINT chk_pr_period  CHECK (pay_period_end >= pay_period_start),
    ADD CONSTRAINT chk_pr_amounts CHECK (base_salary > 0 AND overtime_hours >= 0 AND deductions >= 0 AND net_pay >= 0);

ALTER TABLE performance_records
    ADD CONSTRAINT chk_perf_rating CHECK (rating IS NULL OR rating BETWEEN 1 AND 5);

-- ---------- QA round 2 ----------
ALTER TABLE users
    ADD COLUMN active                BOOLEAN  NOT NULL DEFAULT TRUE,
    ADD COLUMN must_change_password  BOOLEAN  NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_login_attempts INT      NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          DATETIME;

ALTER TABLE employees
    ADD COLUMN gender VARCHAR(10),
    ADD CONSTRAINT chk_emp_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER'));

ALTER TABLE notifications
    ADD COLUMN delivery_status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    ADD COLUMN delivered_at    DATETIME;

CREATE TABLE public_holidays (
    holiday_date        DATE         PRIMARY KEY,
    name                VARCHAR(120) NOT NULL,
    created_by          INT,
    CONSTRAINT fk_hol_creator FOREIGN KEY (created_by) REFERENCES users(user_id)
);

CREATE TABLE employee_salaries (
    employee_id         INT          PRIMARY KEY,
    base_salary         DECIMAL(12,2) NOT NULL,
    updated_by          INT,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_sal_emp     FOREIGN KEY (employee_id) REFERENCES employees(employee_id),
    CONSTRAINT fk_sal_updater FOREIGN KEY (updated_by)  REFERENCES users(user_id),
    CONSTRAINT chk_sal_amount CHECK (base_salary > 0)
);
