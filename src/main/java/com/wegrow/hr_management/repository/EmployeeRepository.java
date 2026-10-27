package com.wegrow.hr_management.repository;

import com.wegrow.hr_management.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByMatricule(String matricule);

    Optional<Employee> findByEmail(String email);

    List<Employee> findByManagerId(Long managerId);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE LOWER(e.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Employee> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT e FROM Employee e WHERE e.department.id = :deptId " +
           "AND (LOWER(e.firstName) LIKE LOWER(CONCAT('%', :name, '%')) " +
           "OR LOWER(e.lastName) LIKE LOWER(CONCAT('%', :name, '%')))")
    Page<Employee> searchByDepartmentAndName(@Param("deptId") Long deptId, @Param("name") String name, Pageable pageable);
}