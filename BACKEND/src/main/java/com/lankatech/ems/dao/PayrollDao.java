package com.lankatech.ems.dao;

import com.lankatech.ems.model.PayrollRecord;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PayrollDao {

    PayrollRecord save(PayrollRecord record);
    Optional<PayrollRecord> findById(int payrollId);
    List<PayrollRecord> findAll();
    List<PayrollRecord> findByEmployee(int employeeId);
    List<PayrollRecord> findActiveOverlapping(int employeeId, LocalDate start, LocalDate end);
    void update(PayrollRecord record);
    void updateStatus(int payrollId, String status);
    int countByStatus(String status);
}
