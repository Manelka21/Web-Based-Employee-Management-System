-- ============================================================
-- Employee Management System — sample data (seed.sql)
-- LankaTech Services (Pvt) Ltd
--
-- Adds realistic demo data for every module: departments, positions,
-- employees, user accounts, recruitment, training, attendance, leave,
-- payroll, performance reviews, notifications and the activity log.
--
-- • Run AFTER schema.sql, in MySQL Workbench or:
--     mysql -u root -p < src/main/resources/seed.sql
-- • Safe to run on an existing database: it only INSERTs rows that don't
--   exist yet (and links accounts / department heads that are still empty).
--   Nothing is deleted or overwritten. Running it twice changes nothing.
-- • Dates are relative to the day you run it (CURDATE()), so the dashboard,
--   attendance and payroll always look current.
-- • Every new account's password is:  password123
--   Payroll figures follow PayrollCalculator: overtime = hours × (base / 160) × 1.5,
--   net pay = base + overtime − deductions (8% EPF).
-- ============================================================

USE ems_db;

-- BCrypt(10) hash of "password123"
SET @pw := '$2a$10$KyTrauGckQJwln/XHZcbk.lojjKRaH4EuUNKpuBgFkzjJIN7nSB8G';

START TRANSACTION;

-- ------------------------------------------------------------
-- 1. Departments
-- ------------------------------------------------------------
INSERT INTO departments (name, description, status)
SELECT s.name, s.description, 'ACTIVE'
FROM (
          SELECT 'Human Resources' AS name, 'HR department — manages recruitment, onboarding, employee records, and compliance' AS description
UNION ALL SELECT 'Engineering',      'Software development and IT operations'
UNION ALL SELECT 'Finance',          'Budgeting, accounts, reconciliations and payroll processing'
UNION ALL SELECT 'Marketing',        'Brand, campaigns and digital marketing'
UNION ALL SELECT 'Customer Support', 'Client help desk, onboarding calls and service quality'
) s
WHERE NOT EXISTS (SELECT 1 FROM departments d WHERE d.name = s.name);

-- ------------------------------------------------------------
-- 2. Job positions
-- ------------------------------------------------------------
INSERT INTO job_positions (title, department_id, description)
SELECT s.title, d.department_id, s.description
FROM (
          SELECT 'HR Manager' AS title, 'Human Resources' AS dept, 'Manages all HR operations' AS description
UNION ALL SELECT 'HR Executive',               'Human Resources',  'Handles onboarding, employee records and HR queries'
UNION ALL SELECT 'Software Engineer',          'Engineering',      'Develops and maintains software applications'
UNION ALL SELECT 'Senior Software Engineer',   'Engineering',      'Designs features end to end and mentors engineers'
UNION ALL SELECT 'Engineering Supervisor',     'Engineering',      'Supervises the engineering team'
UNION ALL SELECT 'QA Engineer',                'Engineering',      'Plans and runs manual and automated tests'
UNION ALL SELECT 'DevOps Engineer',            'Engineering',      'Maintains CI/CD pipelines, servers and monitoring'
UNION ALL SELECT 'Finance Manager',            'Finance',          'Owns budgets, reporting and financial controls'
UNION ALL SELECT 'Accountant',                 'Finance',          'Month-end close, reconciliations and accounts payable'
UNION ALL SELECT 'Payroll Officer',            'Finance',          'Prepares, checks and releases monthly payroll'
UNION ALL SELECT 'Marketing Manager',          'Marketing',        'Leads brand strategy and campaign planning'
UNION ALL SELECT 'Digital Marketing Executive','Marketing',        'Runs paid social, search and email campaigns'
UNION ALL SELECT 'Support Team Lead',          'Customer Support', 'Leads the help desk and coaches support specialists'
UNION ALL SELECT 'Support Specialist',         'Customer Support', 'Resolves client tickets by phone, email and chat'
) s
JOIN departments d ON d.name = s.dept
WHERE NOT EXISTS (SELECT 1 FROM job_positions p WHERE p.title = s.title AND p.department_id = d.department_id);

-- ------------------------------------------------------------
-- 3. Employees
-- ------------------------------------------------------------
INSERT INTO employees (first_name, last_name, nic, email, phone, address, department_id, position_id, hire_date, status)
SELECT s.first_name, s.last_name, s.nic, s.email, s.phone, s.address, d.department_id, p.position_id, s.hire_date, s.status
FROM (
          SELECT 'Tharindu' AS first_name, 'Silva' AS last_name, '199512345678' AS nic, 'employee@lankatech.lk' AS email, '0711111111' AS phone, 'Colombo 05' AS address, 'Engineering' AS dept, 'Software Engineer' AS pos, '2024-01-15' AS hire_date, 'ACTIVE' AS status
UNION ALL SELECT 'Kasun',    'Wickramasinghe', '198812345678', 'supervisor@lankatech.lk',          '0722222222', 'Kandy',        'Engineering',      'Engineering Supervisor',      '2020-03-01', 'ACTIVE'
UNION ALL SELECT 'Nadeesha', 'Perera',         '198703456789', 'hr@lankatech.lk',                  '0773456789', 'Nugegoda',     'Human Resources',  'HR Manager',                  '2019-06-10', 'ACTIVE'
UNION ALL SELECT 'Ruvini',   'Bandara',        '199608765432', 'ruvini.bandara@lankatech.lk',      '0714567890', 'Maharagama',   'Human Resources',  'HR Executive',                '2023-02-01', 'ACTIVE'
UNION ALL SELECT 'Dilini',   'Karunaratne',    '199204567891', 'payroll@lankatech.lk',             '0765678901', 'Dehiwala',     'Finance',          'Payroll Officer',             '2021-09-15', 'ACTIVE'
UNION ALL SELECT 'Priyanka', 'Mendis',         '198511223344', 'priyanka.mendis@lankatech.lk',     '0776789012', 'Rajagiriya',   'Finance',          'Finance Manager',             '2018-04-02', 'ACTIVE'
UNION ALL SELECT 'Kavindu',  'Ekanayake',      '199807654321', 'kavindu.ekanayake@lankatech.lk',   '0707890123', 'Kadawatha',    'Finance',          'Accountant',                  DATE_SUB(CURDATE(), INTERVAL 40 DAY), 'PROBATION'
UNION ALL SELECT 'Sanjaya',  'Fernando',       '197902345678', 'director@lankatech.lk',            '0718901234', 'Colombo 07',   NULL,               NULL,                          '2015-01-05', 'ACTIVE'
UNION ALL SELECT 'Nimali',   'Fernando',       '199409876543', 'nimali.fernando@lankatech.lk',     '0779012345', 'Wattala',      'Engineering',      'Senior Software Engineer',    '2021-11-01', 'ACTIVE'
UNION ALL SELECT 'Dilan',    'Jayawardena',    '200001234567', 'dilan.jayawardena@lankatech.lk',   '0750123456', 'Panadura',     'Engineering',      'QA Engineer',                 DATE_SUB(CURDATE(), INTERVAL 60 DAY), 'PROBATION'
UNION ALL SELECT 'Ishara',   'Gunasekara',     '199312345670', 'ishara.gunasekara@lankatech.lk',   '0781234567', 'Kelaniya',     'Engineering',      'DevOps Engineer',             '2022-07-18', 'ACTIVE'
UNION ALL SELECT 'Sachini',  'Wijesinghe',     '199505551234', 'sachini.wijesinghe@lankatech.lk',  '0712345098', 'Moratuwa',     'Engineering',      'Software Engineer',           '2022-03-07', 'ON_LEAVE'
UNION ALL SELECT 'Shehani',  'Abeysekera',     '199002223344', 'shehani.abeysekera@lankatech.lk',  '0773456120', 'Battaramulla', 'Marketing',        'Marketing Manager',           '2020-08-24', 'ACTIVE'
UNION ALL SELECT 'Ravindu',  'Pathirana',      '199906667788', 'ravindu.pathirana@lankatech.lk',   '0764567231', 'Kottawa',      'Marketing',        'Digital Marketing Executive', DATE_SUB(CURDATE(), INTERVAL 75 DAY), 'PROBATION'
UNION ALL SELECT 'Malith',   'Weerasinghe',    '198909998877', 'malith.weerasinghe@lankatech.lk',  '0715678342', 'Negombo',      'Customer Support', 'Support Team Lead',           '2019-12-02', 'ACTIVE'
UNION ALL SELECT 'Hiruni',   'Ratnayake',      '199707773344', 'hiruni.ratnayake@lankatech.lk',    '0786789453', 'Gampaha',      'Customer Support', 'Support Specialist',          '2023-05-15', 'ACTIVE'
UNION ALL SELECT 'Oshadha',  'Liyanage',       '199803334455', 'oshadha.liyanage@lankatech.lk',    '0707890564', 'Ja-Ela',       'Customer Support', 'Support Specialist',          '2024-02-19', 'ACTIVE'
UNION ALL SELECT 'Tharushi', 'De Silva',       '199610001122', 'tharushi.desilva@lankatech.lk',    '0758901675', 'Kiribathgoda', 'Customer Support', 'Support Specialist',          '2022-10-03', 'INACTIVE'
) s
LEFT JOIN departments d ON d.name = s.dept
LEFT JOIN job_positions p ON p.title = s.pos AND p.department_id = d.department_id
WHERE NOT EXISTS (SELECT 1 FROM employees x WHERE x.email = s.email OR x.nic = s.nic);

-- Department heads (only where none is set)
UPDATE departments d
JOIN (
          SELECT 'Human Resources' AS dept, 'hr@lankatech.lk' AS email
UNION ALL SELECT 'Engineering',      'supervisor@lankatech.lk'
UNION ALL SELECT 'Finance',          'priyanka.mendis@lankatech.lk'
UNION ALL SELECT 'Marketing',        'shehani.abeysekera@lankatech.lk'
UNION ALL SELECT 'Customer Support', 'malith.weerasinghe@lankatech.lk'
) s ON s.dept = d.name
JOIN employees e ON e.email = s.email
SET d.head_of_dept_id = e.employee_id
WHERE d.head_of_dept_id IS NULL;

-- ------------------------------------------------------------
-- 4. User accounts (password: password123)
-- ------------------------------------------------------------
INSERT INTO users (email, password_hash, first_name, last_name, phone_number, role, employee_id)
SELECT s.email, @pw, s.first_name, s.last_name, s.phone, s.role, e.employee_id
FROM (
          SELECT 'hr@lankatech.lk' AS email, 'Nadeesha' AS first_name, 'Perera' AS last_name, '0773456789' AS phone, 'HR_MANAGER' AS role
UNION ALL SELECT 'admin@lankatech.lk',              'Ruwan',    'Jayasinghe',     '0711234000', 'IT_ADMIN'
UNION ALL SELECT 'director@lankatech.lk',           'Sanjaya',  'Fernando',       '0718901234', 'COMPANY_DIRECTOR'
UNION ALL SELECT 'payroll@lankatech.lk',            'Dilini',   'Karunaratne',    '0765678901', 'PAYROLL_EXECUTIVE'
UNION ALL SELECT 'supervisor@lankatech.lk',         'Kasun',    'Wickramasinghe', '0722222222', 'DEPT_SUPERVISOR'
UNION ALL SELECT 'employee@lankatech.lk',           'Tharindu', 'Silva',          '0711111111', 'EMPLOYEE'
UNION ALL SELECT 'shehani.abeysekera@lankatech.lk', 'Shehani',  'Abeysekera',     '0773456120', 'DEPT_SUPERVISOR'
UNION ALL SELECT 'malith.weerasinghe@lankatech.lk', 'Malith',   'Weerasinghe',    '0715678342', 'DEPT_SUPERVISOR'
UNION ALL SELECT 'nimali.fernando@lankatech.lk',    'Nimali',   'Fernando',       '0779012345', 'EMPLOYEE'
UNION ALL SELECT 'hiruni.ratnayake@lankatech.lk',   'Hiruni',   'Ratnayake',      '0786789453', 'EMPLOYEE'
UNION ALL SELECT 'ravindu.pathirana@lankatech.lk',  'Ravindu',  'Pathirana',      '0764567231', 'EMPLOYEE'
UNION ALL SELECT 'dilan.jayawardena@lankatech.lk',  'Dilan',    'Jayawardena',    '0750123456', 'EMPLOYEE'
) s
LEFT JOIN employees e ON e.email = s.email AND s.role <> 'IT_ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = s.email);

-- Link existing accounts to the employee record with the same email (IT admins stay unlinked)
UPDATE users u
JOIN employees e ON e.email = u.email
SET u.employee_id = e.employee_id
WHERE u.employee_id IS NULL AND u.role <> 'IT_ADMIN';

-- ------------------------------------------------------------
-- 5. Recruitment — vacancies and candidate applications
-- ------------------------------------------------------------
INSERT INTO vacancies (title, department_id, requirements, deadline, status, created_by, created_at)
SELECT s.title, d.department_id, s.requirements, s.deadline, s.status,
       (SELECT user_id FROM users WHERE email = 'hr@lankatech.lk'), s.created_at
FROM (
          SELECT 'Senior QA Engineer' AS title, 'Engineering' AS dept,
                 '4+ years in manual and automated testing; Cypress or Selenium; API testing with Postman' AS requirements,
                 DATE_ADD(CURDATE(), INTERVAL 30 DAY) AS deadline, 'OPEN' AS status, DATE_SUB(NOW(), INTERVAL 6 DAY) AS created_at
UNION ALL SELECT 'Digital Marketing Specialist', 'Marketing',
                 '3+ years running paid social and search campaigns; Google Ads certification preferred',
                 DATE_ADD(CURDATE(), INTERVAL 21 DAY), 'OPEN', DATE_SUB(NOW(), INTERVAL 3 DAY)
UNION ALL SELECT 'Customer Support Specialist', 'Customer Support',
                 'Fluent in Sinhala, Tamil and English; 1+ year in a help desk or contact centre role',
                 DATE_SUB(CURDATE(), INTERVAL 5 DAY), 'CLOSED', DATE_SUB(NOW(), INTERVAL 40 DAY)
UNION ALL SELECT 'Accountant', 'Finance',
                 'Part-qualified CIMA or ACCA; experience with month-end close and reconciliations',
                 DATE_SUB(CURDATE(), INTERVAL 70 DAY), 'FILLED', DATE_SUB(NOW(), INTERVAL 100 DAY)
) s
JOIN departments d ON d.name = s.dept
WHERE NOT EXISTS (SELECT 1 FROM vacancies v WHERE v.title = s.title AND v.department_id = d.department_id);

INSERT INTO candidate_applications (vacancy_id, candidate_name, candidate_email, candidate_phone, candidate_nic, resume_notes, status, applied_date)
SELECT v.vacancy_id, s.name, s.email, s.phone, s.nic, s.notes, s.status, s.applied
FROM (
          SELECT 'Senior QA Engineer' AS vacancy, 'Amaya Rathnayake' AS name, 'amaya.rathnayake@gmail.com' AS email, '0771234567' AS phone, '200012345678' AS nic,
                 'Four years at a fintech startup; built the Cypress regression suite from scratch.' AS notes, 'APPLIED' AS status, DATE_SUB(CURDATE(), INTERVAL 2 DAY) AS applied
UNION ALL SELECT 'Senior QA Engineer', 'Pasindu Kumara', 'pasindu.kumara@gmail.com', '0712233445', '199711223344',
                 'ISTQB certified; led regression testing for a mobile banking app.', 'SHORTLISTED', DATE_SUB(CURDATE(), INTERVAL 4 DAY)
UNION ALL SELECT 'Senior QA Engineer', 'Nethmi Jayasuriya', 'nethmi.jayasuriya@outlook.com', '0759988776', '199812340987',
                 'Strong in performance testing (JMeter). Interview panel rated her highly on test design.', 'INTERVIEWED', DATE_SUB(CURDATE(), INTERVAL 5 DAY)
UNION ALL SELECT 'Digital Marketing Specialist', 'Yasiru Senarath', 'yasiru.senarath@gmail.com', '0768877665', '199905432198',
                 'Managed a LKR 2M monthly ad budget for a retail brand.', 'APPLIED', DATE_SUB(CURDATE(), INTERVAL 1 DAY)
UNION ALL SELECT 'Digital Marketing Specialist', 'Kaveesha Perera', 'kaveesha.perera@gmail.com', '0703344556', '200107766554',
                 'Fresh graduate; internship in social media content only.', 'REJECTED', DATE_SUB(CURDATE(), INTERVAL 2 DAY)
UNION ALL SELECT 'Customer Support Specialist', 'Thilini Samaraweera', 'thilini.samaraweera@gmail.com', '0776655443', '199609988776',
                 'Two years at a telco contact centre; trilingual.', 'SHORTLISTED', DATE_SUB(CURDATE(), INTERVAL 30 DAY)
UNION ALL SELECT 'Customer Support Specialist', 'Ashan Silva', 'ashan.silva@yahoo.com', '0715566778', '199703322110',
                 'Accepted another offer before the interview.', 'WITHDRAWN', DATE_SUB(CURDATE(), INTERVAL 35 DAY)
UNION ALL SELECT 'Accountant', 'Kavindu Ekanayake', 'kavindu.ekanayake@gmail.com', '0707890123', '199807654321',
                 'CIMA passed finalist; three years in audit at a mid-size firm.', 'HIRED', DATE_SUB(CURDATE(), INTERVAL 90 DAY)
UNION ALL SELECT 'Accountant', 'Rashmi Gunawardena', 'rashmi.gunawardena@gmail.com', '0782211009', '199501122334',
                 'Good ERP experience but salary expectations above the band.', 'REJECTED', DATE_SUB(CURDATE(), INTERVAL 88 DAY)
) s
JOIN vacancies v ON v.title = s.vacancy
WHERE NOT EXISTS (SELECT 1 FROM candidate_applications a WHERE a.vacancy_id = v.vacancy_id AND a.candidate_email = s.email);

-- ------------------------------------------------------------
-- 6. Training — programs and enrollments
-- ------------------------------------------------------------
INSERT INTO training_programs (title, trainer, start_date, end_date, department_id, capacity, description, status, created_by)
SELECT s.title, s.trainer, s.start_date, s.end_date, d.department_id, s.capacity, s.description, s.status,
       (SELECT user_id FROM users WHERE email = 'hr@lankatech.lk')
FROM (
          SELECT 'Secure Coding Fundamentals' AS title, 'Chathura Wijeratne' AS trainer,
                 DATE_SUB(CURDATE(), INTERVAL 7 DAY) AS start_date, DATE_ADD(CURDATE(), INTERVAL 7 DAY) AS end_date,
                 'Engineering' AS dept, 15 AS capacity, 'OWASP Top 10, input validation, secrets handling and secure code review.' AS description, 'IN_PROGRESS' AS status
UNION ALL SELECT 'Customer Empathy Workshop', 'Anusha Kodituwakku',
                 DATE_ADD(CURDATE(), INTERVAL 14 DAY), DATE_ADD(CURDATE(), INTERVAL 15 DAY),
                 'Customer Support', 20, 'Active listening, de-escalation and writing clear support replies.', 'SCHEDULED'
UNION ALL SELECT 'Leadership Essentials', 'Dr. Mahesh Rodrigo',
                 DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 32 DAY),
                 NULL, 12, 'Coaching conversations, giving feedback and running effective one-to-ones.', 'SCHEDULED'
UNION ALL SELECT 'Advanced Excel for Finance', 'Sunethra Alwis',
                 DATE_SUB(CURDATE(), INTERVAL 60 DAY), DATE_SUB(CURDATE(), INTERVAL 58 DAY),
                 'Finance', 10, 'Pivot tables, Power Query and building reconciliation templates.', 'COMPLETED'
UNION ALL SELECT 'Workplace Health & Safety', 'Safety First Lanka',
                 DATE_ADD(CURDATE(), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY),
                 NULL, 40, 'Fire drills, first aid basics and ergonomic workstation setup.', 'CANCELLED'
) s
LEFT JOIN departments d ON d.name = s.dept
WHERE NOT EXISTS (SELECT 1 FROM training_programs t WHERE t.title = s.title);

INSERT INTO training_enrollments (employee_id, program_id, enrolled_date, completion_status, completion_date)
SELECT e.employee_id, t.program_id, s.enrolled, s.status, s.completed
FROM (
          SELECT 'employee@lankatech.lk' AS email, 'Secure Coding Fundamentals' AS program, DATE_SUB(CURDATE(), INTERVAL 14 DAY) AS enrolled, 'IN_PROGRESS' AS status, CAST(NULL AS DATE) AS completed
UNION ALL SELECT 'nimali.fernando@lankatech.lk',    'Secure Coding Fundamentals', DATE_SUB(CURDATE(), INTERVAL 14 DAY), 'IN_PROGRESS', NULL
UNION ALL SELECT 'ishara.gunasekara@lankatech.lk',  'Secure Coding Fundamentals', DATE_SUB(CURDATE(), INTERVAL 13 DAY), 'IN_PROGRESS', NULL
UNION ALL SELECT 'dilan.jayawardena@lankatech.lk',  'Secure Coding Fundamentals', DATE_SUB(CURDATE(), INTERVAL 12 DAY), 'DROPPED',     NULL
UNION ALL SELECT 'hiruni.ratnayake@lankatech.lk',   'Customer Empathy Workshop',  DATE_SUB(CURDATE(), INTERVAL 2 DAY),  'ENROLLED',    NULL
UNION ALL SELECT 'oshadha.liyanage@lankatech.lk',   'Customer Empathy Workshop',  DATE_SUB(CURDATE(), INTERVAL 2 DAY),  'ENROLLED',    NULL
UNION ALL SELECT 'malith.weerasinghe@lankatech.lk', 'Leadership Essentials',      DATE_SUB(CURDATE(), INTERVAL 5 DAY),  'ENROLLED',    NULL
UNION ALL SELECT 'shehani.abeysekera@lankatech.lk', 'Leadership Essentials',      DATE_SUB(CURDATE(), INTERVAL 4 DAY),  'ENROLLED',    NULL
UNION ALL SELECT 'supervisor@lankatech.lk',         'Leadership Essentials',      DATE_SUB(CURDATE(), INTERVAL 4 DAY),  'ENROLLED',    NULL
UNION ALL SELECT 'priyanka.mendis@lankatech.lk',    'Advanced Excel for Finance', DATE_SUB(CURDATE(), INTERVAL 75 DAY), 'COMPLETED',   DATE_SUB(CURDATE(), INTERVAL 58 DAY)
UNION ALL SELECT 'payroll@lankatech.lk',            'Advanced Excel for Finance', DATE_SUB(CURDATE(), INTERVAL 74 DAY), 'COMPLETED',   DATE_SUB(CURDATE(), INTERVAL 58 DAY)
) s
JOIN employees e ON e.email = s.email
JOIN training_programs t ON t.title = s.program
WHERE NOT EXISTS (SELECT 1 FROM training_enrollments x WHERE x.employee_id = e.employee_id AND x.program_id = t.program_id);

-- ------------------------------------------------------------
-- 7. Leave requests
-- ------------------------------------------------------------
INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, reason, status, approved_by, approved_date, submitted_date)
SELECT e.employee_id, s.leave_type, s.start_date, s.end_date, s.reason, s.status,
       u.user_id, CASE WHEN u.user_id IS NULL THEN NULL ELSE DATE_SUB(NOW(), INTERVAL s.decided_days_ago DAY) END,
       DATE_SUB(NOW(), INTERVAL s.submitted_days_ago DAY)
FROM (
          SELECT 'employee@lankatech.lk' AS email, 'ANNUAL' AS leave_type,
                 DATE_SUB(CURDATE(), INTERVAL 45 DAY) AS start_date, DATE_SUB(CURDATE(), INTERVAL 43 DAY) AS end_date,
                 'Family event in Galle' AS reason, 'APPROVED' AS status, 'supervisor@lankatech.lk' AS approver, 55 AS decided_days_ago, 56 AS submitted_days_ago
UNION ALL SELECT 'nimali.fernando@lankatech.lk', 'ANNUAL', DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 18 DAY),
                 'Trip to Ella with family', 'APPROVED', 'supervisor@lankatech.lk', 30, 31
UNION ALL SELECT 'ishara.gunasekara@lankatech.lk', 'SICK', DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY),
                 'Fever', 'APPROVED', 'supervisor@lankatech.lk', 8, 9
UNION ALL SELECT 'sachini.wijesinghe@lankatech.lk', 'MATERNITY', DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 50 DAY),
                 'Maternity leave', 'APPROVED', 'hr@lankatech.lk', 45, 50
UNION ALL SELECT 'dilan.jayawardena@lankatech.lk', 'SICK', DATE_ADD(CURDATE(), INTERVAL 3 DAY), DATE_ADD(CURDATE(), INTERVAL 4 DAY),
                 'Dental surgery and recovery', 'PENDING', NULL, 0, 1
UNION ALL SELECT 'hiruni.ratnayake@lankatech.lk', 'CASUAL', DATE_ADD(CURDATE(), INTERVAL 6 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY),
                 'Sister''s wedding', 'PENDING', NULL, 0, 2
UNION ALL SELECT 'ravindu.pathirana@lankatech.lk', 'ANNUAL', DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 19 DAY),
                 'Holiday in Nuwara Eliya', 'REJECTED', 'shehani.abeysekera@lankatech.lk', 1, 3
UNION ALL SELECT 'oshadha.liyanage@lankatech.lk', 'CASUAL', DATE_SUB(CURDATE(), INTERVAL 25 DAY), DATE_SUB(CURDATE(), INTERVAL 25 DAY),
                 'Bank appointment — no longer needed', 'CANCELLED', NULL, 0, 30
) s
JOIN employees e ON e.email = s.email
LEFT JOIN users u ON u.email = s.approver
WHERE NOT EXISTS (
    SELECT 1 FROM leave_requests l
    WHERE l.employee_id = e.employee_id AND l.leave_type = s.leave_type AND l.start_date = s.start_date
);

-- ------------------------------------------------------------
-- 8. Attendance — last 4 weeks of weekdays, generated per employee
--    (skips weekends, days before hire, approved leave, inactive and on-leave staff)
-- ------------------------------------------------------------
INSERT INTO attendance_records (employee_id, date, check_in_time, check_out_time, status, override_reason)
WITH RECURSIVE days AS (
    SELECT DATE_SUB(CURDATE(), INTERVAL 1 DAY) AS d
    UNION ALL
    SELECT DATE_SUB(d, INTERVAL 1 DAY) FROM days WHERE d > DATE_SUB(CURDATE(), INTERVAL 28 DAY)
),
grid AS (
    SELECT e.employee_id, days.d, MOD(e.employee_id * 7 + DAYOFYEAR(days.d), 13) AS k
    FROM employees e
    CROSS JOIN days
    WHERE e.status IN ('ACTIVE', 'PROBATION')
      AND e.department_id IS NOT NULL
      AND WEEKDAY(days.d) < 5
      AND days.d >= e.hire_date
      AND NOT EXISTS (
          SELECT 1 FROM leave_requests l
          WHERE l.employee_id = e.employee_id AND l.status = 'APPROVED' AND days.d BETWEEN l.start_date AND l.end_date
      )
)
SELECT g.employee_id,
       g.d,
       CASE WHEN g.k = 0 THEN NULL
            WHEN g.k IN (1, 2) THEN MAKETIME(9, 10 + MOD(g.employee_id * 11 + DAY(g.d), 40), 0)
            ELSE MAKETIME(8, 15 + MOD(g.employee_id * 3 + DAY(g.d), 40), 0) END,
       CASE WHEN g.k = 0 THEN NULL
            WHEN g.k = 3 THEN MAKETIME(13, MOD(DAY(g.d), 30), 0)
            ELSE MAKETIME(17, MOD(g.employee_id * 5 + DAY(g.d), 55), 0) END,
       CASE WHEN g.k = 0 THEN 'ABSENT'
            WHEN g.k IN (1, 2) THEN 'LATE'
            WHEN g.k = 3 THEN 'HALF_DAY'
            ELSE 'PRESENT' END,
       NULL
FROM grid g
WHERE NOT EXISTS (SELECT 1 FROM attendance_records a WHERE a.employee_id = g.employee_id AND a.date = g.d);

-- A few people already checked in today (Tharindu is left out so the Employee demo can check in)
INSERT INTO attendance_records (employee_id, date, check_in_time, check_out_time, status)
SELECT e.employee_id, CURDATE(), s.check_in, NULL, s.status
FROM (
          SELECT 'supervisor@lankatech.lk' AS email, '08:32:00' AS check_in, 'PRESENT' AS status
UNION ALL SELECT 'nimali.fernando@lankatech.lk',    '08:47:00', 'PRESENT'
UNION ALL SELECT 'ishara.gunasekara@lankatech.lk',  '09:14:00', 'LATE'
UNION ALL SELECT 'hiruni.ratnayake@lankatech.lk',   '08:25:00', 'PRESENT'
UNION ALL SELECT 'malith.weerasinghe@lankatech.lk', '08:10:00', 'PRESENT'
UNION ALL SELECT 'shehani.abeysekera@lankatech.lk', '08:55:00', 'PRESENT'
UNION ALL SELECT 'payroll@lankatech.lk',            '08:40:00', 'PRESENT'
) s
JOIN employees e ON e.email = s.email
WHERE NOT EXISTS (SELECT 1 FROM attendance_records a WHERE a.employee_id = e.employee_id AND a.date = CURDATE());

-- One corrected record, so the "override reason" column has an example
-- (only once: skipped if Dilan already has a corrected record)
UPDATE attendance_records a
JOIN (
    SELECT r.employee_id, MAX(r.date) AS corrected_date
    FROM attendance_records r
    JOIN employees e ON e.employee_id = r.employee_id
    WHERE e.email = 'dilan.jayawardena@lankatech.lk'
      AND r.date < CURDATE()
      AND NOT EXISTS (SELECT 1 FROM attendance_records z WHERE z.employee_id = r.employee_id AND z.override_reason IS NOT NULL)
    GROUP BY r.employee_id
) t ON t.employee_id = a.employee_id AND t.corrected_date = a.date
SET a.status = 'PRESENT', a.check_in_time = '08:50:00', a.check_out_time = '17:05:00',
    a.override_reason = 'Biometric reader was offline; confirmed by supervisor';

-- ------------------------------------------------------------
-- 9. Payroll — 2 months ago PAID, last month FINALIZED, a few DRAFTs this month
-- ------------------------------------------------------------
INSERT INTO payroll_records (employee_id, pay_period_start, pay_period_end, base_salary, overtime_hours, overtime_amount, deductions, net_pay, status, generated_by, generated_at)
SELECT x.employee_id, x.period_start, x.period_end, x.base_salary, x.overtime_hours, x.overtime_amount, x.deductions,
       ROUND(x.base_salary + x.overtime_amount - x.deductions, 2),
       x.status, (SELECT user_id FROM users WHERE email = 'payroll@lankatech.lk'), x.generated_at
FROM (
    SELECT e.employee_id, p.period_start, p.period_end, s.base_salary, p.status, p.generated_at,
           MOD(e.employee_id * 3 + MONTH(p.period_start), 9) AS overtime_hours,
           ROUND(ROUND(s.base_salary / 160, 4) * 1.5 * MOD(e.employee_id * 3 + MONTH(p.period_start), 9), 2) AS overtime_amount,
           ROUND(s.base_salary * 0.08, 2) AS deductions
    FROM (
              SELECT 'employee@lankatech.lk' AS email, 75000.00 AS base_salary, 0 AS draft_this_month
    UNION ALL SELECT 'supervisor@lankatech.lk',          145000.00, 0
    UNION ALL SELECT 'hr@lankatech.lk',                  160000.00, 0
    UNION ALL SELECT 'ruvini.bandara@lankatech.lk',       70000.00, 0
    UNION ALL SELECT 'payroll@lankatech.lk',             110000.00, 0
    UNION ALL SELECT 'priyanka.mendis@lankatech.lk',     185000.00, 0
    UNION ALL SELECT 'kavindu.ekanayake@lankatech.lk',    90000.00, 0
    UNION ALL SELECT 'director@lankatech.lk',            350000.00, 0
    UNION ALL SELECT 'nimali.fernando@lankatech.lk',     135000.00, 1
    UNION ALL SELECT 'dilan.jayawardena@lankatech.lk',    70000.00, 0
    UNION ALL SELECT 'ishara.gunasekara@lankatech.lk',   120000.00, 1
    UNION ALL SELECT 'sachini.wijesinghe@lankatech.lk',   95000.00, 0
    UNION ALL SELECT 'shehani.abeysekera@lankatech.lk',  150000.00, 0
    UNION ALL SELECT 'ravindu.pathirana@lankatech.lk',    68000.00, 0
    UNION ALL SELECT 'malith.weerasinghe@lankatech.lk',  115000.00, 0
    UNION ALL SELECT 'hiruni.ratnayake@lankatech.lk',     65000.00, 1
    UNION ALL SELECT 'oshadha.liyanage@lankatech.lk',     60000.00, 0
    ) s
    JOIN employees e ON e.email = s.email
    JOIN (
              SELECT CAST(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 2 MONTH), '%Y-%m-01') AS DATE) AS period_start,
                     LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)) AS period_end,
                     'PAID' AS status, 0 AS drafts_only,
                     TIMESTAMP(DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 2 MONTH)), INTERVAL -2 DAY), '10:30:00') AS generated_at
    UNION ALL SELECT CAST(DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-01') AS DATE),
                     LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)),
                     'FINALIZED', 0,
                     TIMESTAMP(DATE_ADD(LAST_DAY(DATE_SUB(CURDATE(), INTERVAL 1 MONTH)), INTERVAL -2 DAY), '10:30:00')
    UNION ALL SELECT CAST(DATE_FORMAT(CURDATE(), '%Y-%m-01') AS DATE),
                     LAST_DAY(CURDATE()),
                     'DRAFT', 1,
                     NOW()
    ) p ON e.hire_date <= p.period_start AND (p.drafts_only = 0 OR s.draft_this_month = 1)
    WHERE e.status <> 'INACTIVE'
) x
WHERE NOT EXISTS (
    SELECT 1 FROM payroll_records r
    WHERE r.employee_id = x.employee_id AND r.pay_period_start = x.period_start AND r.status <> 'VOIDED'
);

-- ------------------------------------------------------------
-- 10. Performance reviews (written by each team's supervisor)
-- ------------------------------------------------------------
INSERT INTO performance_records (employee_id, supervisor_id, feedback, rating, review_date)
SELECT e.employee_id, u.user_id, s.feedback, s.rating, s.review_date
FROM (
          SELECT 'employee@lankatech.lk' AS email, 'supervisor@lankatech.lk' AS reviewer, 4 AS rating, DATE_SUB(CURDATE(), INTERVAL 30 DAY) AS review_date,
                 'Delivered the leave-balance API ahead of schedule. Keep raising test coverage on new endpoints.' AS feedback
UNION ALL SELECT 'nimali.fernando@lankatech.lk', 'supervisor@lankatech.lk', 5, DATE_SUB(CURDATE(), INTERVAL 25 DAY),
                 'Excellent technical leadership on the payroll module, and patient mentoring of the newer engineers.'
UNION ALL SELECT 'ishara.gunasekara@lankatech.lk', 'supervisor@lankatech.lk', 4, DATE_SUB(CURDATE(), INTERVAL 40 DAY),
                 'Cut deployment time in half with the new pipeline. Document the runbooks so others can cover on-call.'
UNION ALL SELECT 'dilan.jayawardena@lankatech.lk', 'supervisor@lankatech.lk', 3, DATE_SUB(CURDATE(), INTERVAL 10 DAY),
                 'Settling in well during probation. Bug reports need clearer reproduction steps and expected results.'
UNION ALL SELECT 'ravindu.pathirana@lankatech.lk', 'shehani.abeysekera@lankatech.lk', 3, DATE_SUB(CURDATE(), INTERVAL 12 DAY),
                 'Strong creative ideas for the social campaigns. Needs to plan content earlier to meet deadlines.'
UNION ALL SELECT 'hiruni.ratnayake@lankatech.lk', 'malith.weerasinghe@lankatech.lk', 5, DATE_SUB(CURDATE(), INTERVAL 15 DAY),
                 'Highest customer satisfaction score on the team this quarter. Great candidate for the team lead track.'
UNION ALL SELECT 'oshadha.liyanage@lankatech.lk', 'malith.weerasinghe@lankatech.lk', 2, DATE_SUB(CURDATE(), INTERVAL 20 DAY),
                 'Average handling time is well above target. Pairing with Hiruni for two weeks to improve ticket triage.'
) s
JOIN employees e ON e.email = s.email
JOIN users u ON u.email = s.reviewer
WHERE NOT EXISTS (
    SELECT 1 FROM performance_records r
    WHERE r.employee_id = e.employee_id AND r.supervisor_id = u.user_id AND r.review_date = s.review_date
);

-- ------------------------------------------------------------
-- 11. Notifications
-- ------------------------------------------------------------
INSERT INTO notifications (user_id, type, message, sent_at, channel, read_status)
SELECT u.user_id, s.type, s.message, DATE_SUB(NOW(), INTERVAL s.hours_ago HOUR), 'WEBSITE', s.is_read
FROM (
          SELECT 'employee@lankatech.lk' AS email, 'PAYSLIP_AVAILABLE' AS type, 'Your payslip for last month is available' AS message, 30 AS hours_ago, FALSE AS is_read
UNION ALL SELECT 'employee@lankatech.lk',           'PERFORMANCE_REVIEW', 'Your supervisor recorded a new performance review',                 720,  FALSE
UNION ALL SELECT 'employee@lankatech.lk',           'TRAINING_ENROLLED',  'You are enrolled in Secure Coding Fundamentals',                    336,  TRUE
UNION ALL SELECT 'employee@lankatech.lk',           'LEAVE_APPROVED',     'Your annual leave request was approved',                            1320, TRUE
UNION ALL SELECT 'nimali.fernando@lankatech.lk',    'PAYSLIP_AVAILABLE',  'Your payslip for last month is available',                          30,   FALSE
UNION ALL SELECT 'nimali.fernando@lankatech.lk',    'LEAVE_APPROVED',     'Your annual leave request was approved',                            720,  TRUE
UNION ALL SELECT 'hiruni.ratnayake@lankatech.lk',   'PERFORMANCE_REVIEW', 'Your supervisor recorded a new performance review',                 360,  FALSE
UNION ALL SELECT 'ravindu.pathirana@lankatech.lk',  'LEAVE_REJECTED',     'Your annual leave request was rejected: overlaps with the campaign launch', 24, FALSE
UNION ALL SELECT 'supervisor@lankatech.lk',         'LEAVE_REQUEST',      'Dilan Jayawardena requested sick leave',                            20,   FALSE
UNION ALL SELECT 'malith.weerasinghe@lankatech.lk', 'LEAVE_REQUEST',      'Hiruni Ratnayake requested casual leave',                           48,   FALSE
UNION ALL SELECT 'hr@lankatech.lk',                 'APPLICATION_RECEIVED','New application from Amaya Rathnayake for Senior QA Engineer',     48,   FALSE
UNION ALL SELECT 'hr@lankatech.lk',                 'APPLICATION_RECEIVED','New application from Yasiru Senarath for Digital Marketing Specialist', 20, FALSE
UNION ALL SELECT 'payroll@lankatech.lk',            'PAYROLL_REMINDER',   'Draft payroll records for this month are waiting to be finalized',  6,    FALSE
UNION ALL SELECT 'director@lankatech.lk',           'REPORT_READY',       'Last month''s headcount and payroll summaries are ready',            52,   TRUE
) s
JOIN users u ON u.email = s.email
WHERE NOT EXISTS (SELECT 1 FROM notifications n WHERE n.user_id = u.user_id AND n.message = s.message);

-- ------------------------------------------------------------
-- 12. Activity log (audit trail for the IT admin)
-- ------------------------------------------------------------
INSERT INTO activity_logs (user_id, action, details, ip_address, timestamp)
SELECT u.user_id, s.action, s.details, s.ip, DATE_SUB(NOW(), INTERVAL s.hours_ago HOUR)
FROM (
          SELECT 'admin@lankatech.lk' AS email, 'CREATE_USER' AS action, 'Created user nimali.fernando@lankatech.lk with role EMPLOYEE' AS details, '192.168.1.10' AS ip, 900 AS hours_ago
UNION ALL SELECT 'admin@lankatech.lk',              'LINK_EMPLOYEE',          'Linked user shehani.abeysekera@lankatech.lk to an employee record', '192.168.1.10', 880
UNION ALL SELECT 'hr@lankatech.lk',                 'UPDATE_APPLICATION_STATUS', 'Application for Accountant moved to HIRED — employee record created', '192.168.1.24', 960
UNION ALL SELECT 'hr@lankatech.lk',                 'CREATE_VACANCY',         'Created vacancy Senior QA Engineer',                     '192.168.1.24', 144
UNION ALL SELECT 'hr@lankatech.lk',                 'CREATE_TRAINING_PROGRAM','Created training program Leadership Essentials',          '192.168.1.24', 200
UNION ALL SELECT 'supervisor@lankatech.lk',         'APPROVE_LEAVE',          'Approved annual leave for Nimali Fernando',              '192.168.1.31', 720
UNION ALL SELECT 'shehani.abeysekera@lankatech.lk', 'REJECT_LEAVE',           'Rejected annual leave for Ravindu Pathirana',            '192.168.1.42', 24
UNION ALL SELECT 'payroll@lankatech.lk',            'FINALIZED_PAYROLL',      'Finalized last month''s payroll records',                 '192.168.1.18', 30
UNION ALL SELECT 'director@lankatech.lk',           'LOGIN',                  'Logged in as director@lankatech.lk',                     '192.168.1.5',  52
UNION ALL SELECT 'payroll@lankatech.lk',            'LOGIN',                  'Logged in as payroll@lankatech.lk',                      '192.168.1.18', 6
) s
JOIN users u ON u.email = s.email
WHERE NOT EXISTS (SELECT 1 FROM activity_logs a WHERE a.user_id = u.user_id AND a.action = s.action AND a.details = s.details);

-- ------------------------------------------------------------
-- 13. Gender (needed for maternity leave; only where not set yet)
-- ------------------------------------------------------------
UPDATE employees SET gender = 'FEMALE'
WHERE gender IS NULL AND email IN (
    'hr@lankatech.lk', 'ruvini.bandara@lankatech.lk', 'payroll@lankatech.lk', 'priyanka.mendis@lankatech.lk',
    'nimali.fernando@lankatech.lk', 'sachini.wijesinghe@lankatech.lk', 'shehani.abeysekera@lankatech.lk',
    'hiruni.ratnayake@lankatech.lk', 'tharushi.desilva@lankatech.lk');

UPDATE employees SET gender = 'MALE'
WHERE gender IS NULL AND email IN (
    'employee@lankatech.lk', 'supervisor@lankatech.lk', 'kavindu.ekanayake@lankatech.lk', 'director@lankatech.lk',
    'dilan.jayawardena@lankatech.lk', 'ravindu.pathirana@lankatech.lk', 'malith.weerasinghe@lankatech.lk',
    'oshadha.liyanage@lankatech.lk');
-- Ishara Gunasekara is left blank on purpose: HR records it from the Directory.

-- ------------------------------------------------------------
-- 14. Public holidays — fixed-date national holidays for this year and next.
--     Poya and other lunar holidays change every year: HR adds them under
--     Organization → Holidays.
-- ------------------------------------------------------------
INSERT INTO public_holidays (holiday_date, name, created_by)
SELECT STR_TO_DATE(CONCAT(y.yr, s.md), '%Y-%m-%d'), s.name, (SELECT user_id FROM users WHERE email = 'hr@lankatech.lk')
FROM (SELECT YEAR(CURDATE()) AS yr UNION ALL SELECT YEAR(CURDATE()) + 1) y
CROSS JOIN (
          SELECT '-02-04' AS md, 'National Day' AS name
UNION ALL SELECT '-05-01', 'May Day'
UNION ALL SELECT '-12-25', 'Christmas Day'
) s
WHERE NOT EXISTS (SELECT 1 FROM public_holidays h WHERE h.holiday_date = STR_TO_DATE(CONCAT(y.yr, s.md), '%Y-%m-%d'));

-- ------------------------------------------------------------
-- 15. Salaries on file — taken from each employee's latest payroll record
-- ------------------------------------------------------------
INSERT INTO employee_salaries (employee_id, base_salary, updated_by)
SELECT p.employee_id, p.base_salary, p.generated_by
FROM payroll_records p
WHERE p.status <> 'VOIDED'
  AND p.pay_period_start = (SELECT MAX(q.pay_period_start) FROM payroll_records q WHERE q.employee_id = p.employee_id AND q.status <> 'VOIDED')
  AND NOT EXISTS (SELECT 1 FROM employee_salaries s WHERE s.employee_id = p.employee_id);

COMMIT;

-- ------------------------------------------------------------
-- Summary
-- ------------------------------------------------------------
SELECT 'departments' AS table_name, COUNT(*) AS total_rows FROM departments
UNION ALL SELECT 'job_positions',          COUNT(*) FROM job_positions
UNION ALL SELECT 'employees',              COUNT(*) FROM employees
UNION ALL SELECT 'users',                  COUNT(*) FROM users
UNION ALL SELECT 'vacancies',              COUNT(*) FROM vacancies
UNION ALL SELECT 'candidate_applications', COUNT(*) FROM candidate_applications
UNION ALL SELECT 'training_programs',      COUNT(*) FROM training_programs
UNION ALL SELECT 'training_enrollments',   COUNT(*) FROM training_enrollments
UNION ALL SELECT 'attendance_records',     COUNT(*) FROM attendance_records
UNION ALL SELECT 'leave_requests',         COUNT(*) FROM leave_requests
UNION ALL SELECT 'payroll_records',        COUNT(*) FROM payroll_records
UNION ALL SELECT 'performance_records',    COUNT(*) FROM performance_records
UNION ALL SELECT 'notifications',          COUNT(*) FROM notifications
UNION ALL SELECT 'activity_logs',          COUNT(*) FROM activity_logs
UNION ALL SELECT 'public_holidays',        COUNT(*) FROM public_holidays
UNION ALL SELECT 'employee_salaries',      COUNT(*) FROM employee_salaries;
