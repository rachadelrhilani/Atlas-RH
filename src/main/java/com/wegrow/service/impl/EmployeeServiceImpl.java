package com.wegrow.service.impl;

import com.wegrow.dto.request.EmployeeRequestDTO;
import com.wegrow.dto.response.EmployeeResponseDTO;
import com.wegrow.dto.response.LeaveBalanceResponseDTO;
import com.wegrow.dto.response.SeniorityResponseDTO;
import com.wegrow.exception.ResourceAlreadyExistsException;
import com.wegrow.exception.ResourceNotFoundException;
import com.wegrow.mapper.EmployeeMapper;
import com.wegrow.model.*;
import com.wegrow.repository.DepartmentRepository;
import com.wegrow.repository.EmployeeRepository;
import com.wegrow.repository.LeaveRequestRepository;
import com.wegrow.repository.LeaveTypeRepository;
import com.wegrow.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeMapper employeeMapper;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            LeaveTypeRepository leaveTypeRepository,
            LeaveRequestRepository leaveRequestRepository,
            EmployeeMapper employeeMapper) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeMapper = employeeMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> findAll(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(employeeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EmployeeResponseDTO> search(String name, Long departmentId, Pageable pageable) {
        if (departmentId != null && name != null && !name.isBlank()) {
            return employeeRepository.searchByDepartmentAndName(departmentId, name, pageable)
                    .map(employeeMapper::toDto);
        } else if (departmentId != null) {
            return employeeRepository.findByDepartmentId(departmentId, pageable)
                    .map(employeeMapper::toDto);
        } else if (name != null && !name.isBlank()) {
            return employeeRepository.searchByName(name, pageable)
                    .map(employeeMapper::toDto);
        }
        return employeeRepository.findAll(pageable).map(employeeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDTO findById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'ID : " + id));
        return employeeMapper.toDto(employee);
    }

    @Override
    public EmployeeResponseDTO create(EmployeeRequestDTO dto) {
        if (employeeRepository.findByMatricule(dto.getMatricule()).isPresent()) {
            throw new ResourceAlreadyExistsException("Le matricule '" + dto.getMatricule() + "' est déjà attribué.");
        }
        if (employeeRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("L'adresse e-mail '" + dto.getEmail() + "' est déjà utilisée.");
        }

        Employee employee = employeeMapper.toEntity(dto);

        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Département non trouvé avec l'ID : " + dto.getDepartmentId()));
            employee.setDepartment(dept);
        }

        if (dto.getManagerId() != null) {
            Employee manager = employeeRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Manager non trouvé avec l'ID : " + dto.getManagerId()));
            employee.setManager(manager);
        }

        return employeeMapper.toDto(employeeRepository.save(employee));
    }

    @Override
    public EmployeeResponseDTO update(Long id, EmployeeRequestDTO dto) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'ID : " + id));

        employeeRepository.findByMatricule(dto.getMatricule()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResourceAlreadyExistsException("Le matricule '" + dto.getMatricule() + "' est déjà utilisé.");
            }
        });
        employeeRepository.findByEmail(dto.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResourceAlreadyExistsException("L'e-mail '" + dto.getEmail() + "' est déjà utilisé.");
            }
        });

        if (dto.getManagerId() != null && dto.getManagerId().equals(id)) {
            throw new IllegalArgumentException("Un employé ne peut pas être son propre manager.");
        }

        employeeMapper.updateEntityFromDto(dto, employee);

        if (dto.getDepartmentId() != null) {
            Department dept = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Département non trouvé : " + dto.getDepartmentId()));
            employee.setDepartment(dept);
        }

        if (dto.getManagerId() != null) {
            Employee manager = employeeRepository.findById(dto.getManagerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Manager non trouvé : " + dto.getManagerId()));
            employee.setManager(manager);
        } else {
            employee.setManager(null);
        }

        return employeeMapper.toDto(employeeRepository.save(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDTO> findTeamByManager(Long managerId) {
        if (!employeeRepository.existsById(managerId)) {
            throw new ResourceNotFoundException("Manager non trouvé avec l'ID : " + managerId);
        }
        return employeeRepository.findByManagerId(managerId).stream()
                .map(employeeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SeniorityResponseDTO getSeniority(Long employeeId) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'ID : " + employeeId));

        if (employee.getHireDate() == null) {
            throw new IllegalStateException("L'employé ne possède pas de date d'embauche.");
        }

        Period period = Period.between(employee.getHireDate(), LocalDate.now());
        SeniorityResponseDTO dto = new SeniorityResponseDTO();
        dto.setYears(period.getYears());
        dto.setMonths(period.getMonths());
        dto.setFormattedSeniority(period.getYears() + " an(s) et " + period.getMonths() + " mois");
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponseDTO> getLeaveBalances(Long employeeId) {

        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employé non trouvé avec l'ID : " + employeeId);
        }

        List<LeaveType> leaveTypes = leaveTypeRepository.findAll();
        List<LeaveRequest> approvedRequests = leaveRequestRepository.findByEmployeeIdAndStatus(employeeId,
                LeaveStatus.APPROUVEE);
        int currentYear = LocalDate.now().getYear();

        List<LeaveBalanceResponseDTO> balances = new ArrayList<>();

        for (LeaveType type : leaveTypes) {
            int usedDays = approvedRequests.stream()
                    .filter(req -> req.getLeaveType().getId().equals(type.getId()))
                    .filter(req -> req.getStartDate().getYear() == currentYear)
                    .mapToInt(LeaveRequest::getWorkingDays)
                    .sum();

            LeaveBalanceResponseDTO balance = new LeaveBalanceResponseDTO();
            balance.setLeaveTypeId(type.getId());
            balance.setLeaveTypeName(type.getName());
            balance.setTotalDaysAllowed(type.getDaysPerYear());
            balance.setDaysUsed(usedDays);
            balance.setRemainingDays(type.getDaysPerYear() - usedDays);

            balances.add(balance);
        }

        return balances;
    }
}