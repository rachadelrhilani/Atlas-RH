package com.wegrow.hr_management.service;

import com.wegrow.hr_management.dto.request.LeaveApprovalDTO;
import com.wegrow.hr_management.dto.request.LeaveRequestCreateDTO;
import com.wegrow.hr_management.dto.response.LeaveRequestResponseDTO;
import com.wegrow.hr_management.mapper.LeaveRequestMapper;
import com.wegrow.hr_management.model.*;
import com.wegrow.hr_management.repository.EmployeeRepository;
import com.wegrow.hr_management.repository.LeaveRequestRepository;
import com.wegrow.hr_management.repository.LeaveTypeRepository;
import com.wegrow.hr_management.service.impl.LeaveRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceImplTest{

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;
    @Mock
    private LeaveRequestMapper leaveRequestMapper;

    @InjectMocks
    private LeaveRequestServiceImpl leaveRequestService;

    private Employee employee;
    private Employee manager;
    private LeaveType leaveType;
    private LeaveRequest leaveRequest;

    @BeforeEach
    void setUp(){
        manager = new Employee();
        manager.setId(1L);

        employee = new Employee();
        employee.setId(2L);
        employee.setManager(manager);

        leaveType = new LeaveType();
        leaveType.setId(1L);
        leaveType.setName("Congé Payé");
        leaveType.setDaysPerYear(22);

        leaveRequest = new LeaveRequest();
        leaveRequest.setId(100L);
        leaveRequest.setEmployee(employee);
        leaveRequest.setLeaveType(leaveType);
        leaveRequest.setStatus(LeaveStatus.EN_ATTENTE);
        leaveRequest.setWorkingDays(5);
    }

    @Nested
    @DisplayName("Règles : Calcul Jours Ouvrés & Solde de Congés")
    class LeaveBalanceAndWorkingDays{

        @Test
        @DisplayName("Succès - Création avec calcul exact des jours ouvrés (Lundi à Vendredi = 5 jours)")
        void createRequest_CalculatesWorkingDays_Success(){
            LeaveRequestCreateDTO dto = new LeaveRequestCreateDTO();
            dto.setEmployeeId(2L);
            dto.setLeaveTypeId(1L);
            dto.setStartDate(LocalDate.of(2026, 11, 2));
            dto.setEndDate(LocalDate.of(2026, 11, 8));

            when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
            when(leaveRequestRepository.findByEmployeeIdAndStatus(2L, LeaveStatus.APPROUVEE)).thenReturn(List.of());
            when(leaveRequestRepository.findByEmployeeId(2L)).thenReturn(List.of());
            when(leaveRequestMapper.toEntity(dto)).thenReturn(leaveRequest);
            when(leaveRequestRepository.save(any())).thenReturn(leaveRequest);
            when(leaveRequestMapper.toDto(any())).thenReturn(new LeaveRequestResponseDTO());

            LeaveRequestResponseDTO result = leaveRequestService.createRequest(dto);

            assertNotNull(result);
            assertEquals(5, leaveRequest.getWorkingDays()); // Vérification du calcul hors week-end
            verify(leaveRequestRepository).save(leaveRequest);
        }

        @Test
        @DisplayName("Erreur - Solde insuffisant (IllegalArgumentException)")
        void createRequest_InsufficientBalance_ThrowsException(){
            LeaveRequestCreateDTO dto = new LeaveRequestCreateDTO();
            dto.setEmployeeId(2L);
            dto.setLeaveTypeId(1L);
            dto.setStartDate(LocalDate.of(2026, 11, 2));
            dto.setEndDate(LocalDate.of(2026, 11, 6)); // 5 jours

            LeaveRequest approved = new LeaveRequest();
            approved.setLeaveType(leaveType);
            approved.setWorkingDays(20);

            when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee));
            when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(leaveType));
            when(leaveRequestRepository.findByEmployeeIdAndStatus(2L, LeaveStatus.APPROUVEE)).thenReturn(List.of(approved));

            assertThrows(IllegalArgumentException.class, () -> leaveRequestService.createRequest(dto));
            verify(leaveRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Règle : Décision du Manager")
    class ManagerDecisionRules{

        @Test
        @DisplayName("Succès - Approbation de la demande par le Manager")
        void processDecision_Approve_Success(){
            LeaveApprovalDTO approvalDTO = new LeaveApprovalDTO();
            approvalDTO.setStatus(LeaveStatus.APPROUVEE);
            approvalDTO.setManagerComment("Accordé");

            when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any())).thenReturn(leaveRequest);
            when(leaveRequestMapper.toDto(any())).thenReturn(new LeaveRequestResponseDTO());

            LeaveRequestResponseDTO result = leaveRequestService.processRequestDecision(100L, 1L, approvalDTO);

            assertNotNull(result);
            assertEquals(LeaveStatus.APPROUVEE, leaveRequest.getStatus());
            assertEquals("Accordé", leaveRequest.getManagerComment());
        }

        @Test
        @DisplayName("Erreur - Impossible de retraiter une demande déjà validée (IllegalStateException)")
        void processDecision_AlreadyProcessed_ThrowsException(){
            leaveRequest.setStatus(LeaveStatus.APPROUVEE); 

            LeaveApprovalDTO approvalDTO = new LeaveApprovalDTO();
            approvalDTO.setStatus(LeaveStatus.REFUSEE);

            when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(leaveRequest));

            assertThrows(IllegalStateException.class, () -> leaveRequestService.processRequestDecision(100L, 1L, approvalDTO));
            verify(leaveRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Règle : Annulation d'une demande")
    class CancelRules{

        @Test
        @DisplayName("Succès - Annulation par l'employé d'une demande en attente")
        void cancelRequest_Success(){
            when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(leaveRequest));
            when(leaveRequestRepository.save(any())).thenReturn(leaveRequest);
            when(leaveRequestMapper.toDto(any())).thenReturn(new LeaveRequestResponseDTO());

            LeaveRequestResponseDTO result = leaveRequestService.cancelRequest(100L, 2L);

            assertNotNull(result);
            assertEquals(LeaveStatus.ANNULEE, leaveRequest.getStatus());
        }

        @Test
        @DisplayName("Erreur - Impossible d'annuler une demande déjà approuvée (IllegalStateException)")
        void cancelRequest_AlreadyApproved_ThrowsException(){
            leaveRequest.setStatus(LeaveStatus.APPROUVEE);

            when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(leaveRequest));

            assertThrows(IllegalStateException.class, () -> leaveRequestService.cancelRequest(100L, 2L));
            verify(leaveRequestRepository, never()).save(any());
        }
    }
}