package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.DepartmentDao;
import com.lankatech.ems.dao.impl.rowmapper.DepartmentRowMapper;
import com.lankatech.ems.model.Department;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
public class DepartmentDaoImpl extends AbstractJdbcDao<Department, Integer> implements DepartmentDao {

    private final DepartmentRowMapper mapper = new DepartmentRowMapper();

    public DepartmentDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Department save(Department d) {
        String sql = "INSERT INTO departments (name, description, head_of_dept_id, status) VALUES (?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            d.getName(), d.getDescription(), d.getHeadOfDeptId(),
            d.getStatus() != null ? d.getStatus() : "ACTIVE"
        );
        d.setDepartmentId(id);
        return d;
    }

    @Override
    public Optional<Department> findById(int departmentId) {
        return queryOne("SELECT * FROM departments WHERE department_id = ?", mapper, departmentId);
    }

    @Override
    public Optional<Department> findByName(String name) {
        return queryOne("SELECT * FROM departments WHERE name = ?", mapper, name);
    }

    @Override
    public List<Department> findAll() {
        return queryList("SELECT * FROM departments ORDER BY department_id", mapper);
    }

    @Override
    public void update(Department d) {
        String sql = "UPDATE departments SET name = ?, description = ?, head_of_dept_id = ?, status = ? WHERE department_id = ?";
        executeUpdate(sql, d.getName(), d.getDescription(), d.getHeadOfDeptId(), d.getStatus(), d.getDepartmentId());
    }

    @Override
    public void updateStatus(int departmentId, String status) {
        executeUpdate("UPDATE departments SET status = ? WHERE department_id = ?", status, departmentId);
    }

    @Override
    public boolean existsByName(String name) {
        return findByName(name).isPresent();
    }

    @Override
    public int countVacancies(int departmentId) {
        return queryForInt("SELECT COUNT(*) FROM vacancies WHERE department_id = ?", departmentId);
    }

    @Override
    public int countTrainingPrograms(int departmentId) {
        return queryForInt("SELECT COUNT(*) FROM training_programs WHERE department_id = ?", departmentId);
    }

    @Override
    public void deleteWithPositions(int departmentId) {
        executeUpdate("DELETE FROM job_positions WHERE department_id = ?", departmentId);
        executeUpdate("DELETE FROM departments WHERE department_id = ?", departmentId);
    }
}
