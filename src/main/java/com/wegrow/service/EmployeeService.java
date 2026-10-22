package com.wegrow.service;

import com.wegrow.dto.request.EmployeeRequestDTO;
import com.wegrow.dto.response.EmployeeResponseDTO;
import com.wegrow.dto.response.LeaveBalanceResponseDTO;
import com.wegrow.dto.response.SeniorityResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface EmployeeService {
    Page<EmployeeResponseDTO> findAll(Pageable pageable);
    Page<EmployeeResponseDTO> search(String name, Long departmentId, Pageable pageable);
    EmployeeResponseDTO findById(Long id);
    EmployeeResponseDTO create(EmployeeRequestDTO dto);
    EmployeeResponseDTO update(Long id, EmployeeRequestDTO dto);
    List<EmployeeResponseDTO> findTeamByManager(Long managerId);
    SeniorityResponseDTO getSeniority(Long employeeId);
    List<LeaveBalanceResponseDTO> getLeaveBalances(Long employeeId);
}