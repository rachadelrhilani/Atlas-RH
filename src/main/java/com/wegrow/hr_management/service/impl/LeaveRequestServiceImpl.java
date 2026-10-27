package com.wegrow.hr_management.service.impl;

import com.wegrow.hr_management.dto.request.LeaveApprovalDTO;
import com.wegrow.hr_management.dto.request.LeaveRequestCreateDTO;
import com.wegrow.hr_management.dto.response.LeaveRequestResponseDTO;
import com.wegrow.hr_management.mapper.LeaveRequestMapper;
import com.wegrow.hr_management.model.*;
import com.wegrow.hr_management.repository.*;
import com.wegrow.hr_management.service.LeaveRequestService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveRequestMapper leaveRequestMapper;

    public LeaveRequestServiceImpl(LeaveRequestRepository leaveRequestRepository,
                                  EmployeeRepository employeeRepository,
                                  LeaveTypeRepository leaveTypeRepository,
                                  LeaveRequestMapper leaveRequestMapper) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveTypeRepository = leaveTypeRepository;
        this.leaveRequestMapper = leaveRequestMapper;
    }

    @Override
    public LeaveRequestResponseDTO createRequest(LeaveRequestCreateDTO dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("La date de fin doit être égale ou postérieure à la date de début.");
        }

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employé introuvable avec l'ID: " + dto.getEmployeeId()));

        LeaveType leaveType = leaveTypeRepository.findById(dto.getLeaveTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Type de congé introuvable avec l'ID: " + dto.getLeaveTypeId()));

        int requestedDays = calculateWorkingDays(dto.getStartDate(), dto.getEndDate());

        List<LeaveRequest> approvedRequests = leaveRequestRepository.findByEmployeeIdAndStatus(employee.getId(), LeaveStatus.APPROUVEE);
        int usedDays = approvedRequests.stream()
                .filter(r -> r.getLeaveType().getId().equals(leaveType.getId()))
                .mapToInt(LeaveRequest::getWorkingDays)
                .sum();

        int remainingDays = leaveType.getDaysPerYear() - usedDays;
        if (requestedDays > remainingDays) {
            throw new IllegalArgumentException("Solde insuffisant pour ce type de congé. Jours restants: " + remainingDays);
        }

        List<LeaveRequest> existingRequests = leaveRequestRepository.findByEmployeeId(employee.getId());
        boolean hasOverlap = existingRequests.stream()
                .filter(r -> r.getStatus() != LeaveStatus.ANNULEE && r.getStatus() != LeaveStatus.REFUSEE)
                .anyMatch(r -> !(dto.getEndDate().isBefore(r.getStartDate()) || dto.getStartDate().isAfter(r.getEndDate())));

        if (hasOverlap) {
            throw new IllegalArgumentException("Une demande de congé existe déjà sur cette période.");
        }

        LeaveRequest request = leaveRequestMapper.toEntity(dto);
        request.setWorkingDays(requestedDays);
        request.setStatus(LeaveStatus.EN_ATTENTE);
        request.setEmployee(employee);
        request.setLeaveType(leaveType);

        LeaveRequest saved = leaveRequestRepository.save(request);
        return leaveRequestMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDTO> getEmployeeRequests(Long employeeId, LeaveStatus status) {
        List<LeaveRequest> requests;
        if (status != null) {
            requests = leaveRequestRepository.findByEmployeeIdAndStatus(employeeId, status);
        } else {
            requests = leaveRequestRepository.findByEmployeeId(employeeId);
        }
        return requests.stream().map(leaveRequestMapper::toDto).toList();
    }

    @Override
    public LeaveRequestResponseDTO cancelRequest(Long requestId, Long employeeId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Demande de congé introuvable avec l'ID: " + requestId));

        if (!request.getEmployee().getId().equals(employeeId)) {
            throw new IllegalArgumentException("Vous ne pouvez annuler que vos propres demandes de congé.");
        }

        if (request.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new IllegalStateException("Seules les demandes en attente peuvent être annulées.");
        }

        request.setStatus(LeaveStatus.ANNULEE);
        return leaveRequestMapper.toDto(leaveRequestRepository.save(request));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveRequestResponseDTO> getPendingTeamRequests(Long managerId) {
        List<Employee> teamMembers = employeeRepository.findByManagerId(managerId);
        if (teamMembers.isEmpty()) {
            return List.of();
        }

        List<Long> teamIds = teamMembers.stream().map(Employee::getId).toList();
        List<LeaveRequest> pendingRequests = leaveRequestRepository.findByEmployeeIdInAndStatus(teamIds, LeaveStatus.EN_ATTENTE);

        return pendingRequests.stream().map(leaveRequestMapper::toDto).toList();
    }

    @Override
    public LeaveRequestResponseDTO processRequestDecision(Long requestId, Long managerId, LeaveApprovalDTO approvalDTO) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Demande de congé introuvable avec l'ID: " + requestId));

        if (request.getEmployee().getManager() == null || !request.getEmployee().getManager().getId().equals(managerId)) {
            throw new IllegalArgumentException("Cette demande de congé ne relève pas de l'équipe de ce manager.");
        }

        if (request.getStatus() != LeaveStatus.EN_ATTENTE) {
            throw new IllegalStateException("Cette demande a déjà été traitée.");
        }

        if (approvalDTO.getStatus() != LeaveStatus.APPROUVEE && approvalDTO.getStatus() != LeaveStatus.REFUSEE) {
            throw new IllegalArgumentException("La décision doit être APPROUVEE ou REFUSEE.");
        }

        request.setStatus(approvalDTO.getStatus());
        request.setManagerComment(approvalDTO.getManagerComment());
        request.setDecisionDate(LocalDate.now());

        LeaveRequest updated = leaveRequestRepository.save(request);
        return leaveRequestMapper.toDto(updated);
    }

    private int calculateWorkingDays(LocalDate start, LocalDate end) {
        int workDays = 0;
        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            if (curr.getDayOfWeek() != DayOfWeek.SATURDAY && curr.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workDays++;
            }
            curr = curr.plusDays(1);
        }
        return workDays;
    }
}