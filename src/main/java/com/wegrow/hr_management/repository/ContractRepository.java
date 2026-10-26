package com.wegrow.hr_management.repository;

import com.wegrow.hr_management.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    List<Contract> findByEmployeeId(Long employeeId);
}