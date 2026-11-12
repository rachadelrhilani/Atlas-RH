package com.wegrow.hr_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegrow.hr_management.dto.request.LeaveApprovalDTO;
import com.wegrow.hr_management.dto.response.LeaveRequestResponseDTO;
import com.wegrow.hr_management.exception.ResourceNotFoundException;
import com.wegrow.hr_management.model.LeaveStatus;
import com.wegrow.hr_management.service.LeaveRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerController.class)
class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveRequestService leaveRequestService;

    private LeaveRequestResponseDTO pendingRequestDTO;
    private LeaveApprovalDTO approvalDTO;

    @BeforeEach
    void setUp() {
        pendingRequestDTO = new LeaveRequestResponseDTO();
        pendingRequestDTO.setId(10L);
        pendingRequestDTO.setEmployeeId(2L);
        pendingRequestDTO.setEmployeeFullName("Rachad El Rhilani");
        pendingRequestDTO.setLeaveTypeName("Congé Payé");
        pendingRequestDTO.setStartDate(LocalDate.of(2026, 11, 16));
        pendingRequestDTO.setEndDate(LocalDate.of(2026, 11, 20));
        pendingRequestDTO.setWorkingDays(5);
        pendingRequestDTO.setStatus(LeaveStatus.EN_ATTENTE);

        approvalDTO = new LeaveApprovalDTO();
        approvalDTO.setStatus(LeaveStatus.APPROUVEE);
        approvalDTO.setManagerComment("Demande approuvée, bon congé.");
    }

    @Nested
    @DisplayName("GET /api/v1/manager/{managerId}/pending-requests - Demandes en attente de l'équipe")
    class GetPendingTeamRequestsTests {

        @Test
        @DisplayName("200 OK - Retourne la liste JSON des demandes en attente")
        void getPendingTeamRequests_Success_Returns200() throws Exception {
            when(leaveRequestService.getPendingTeamRequests(1L)).thenReturn(List.of(pendingRequestDTO));

            mockMvc.perform(get("/api/v1/manager/1/pending-requests"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(10L))
                    .andExpect(jsonPath("$[0].employeeFullName").value("Rachad El Rhilani"))
                    .andExpect(jsonPath("$[0].status").value("EN_ATTENTE"))
                    .andExpect(jsonPath("$[0].workingDays").value(5));

            verify(leaveRequestService, times(1)).getPendingTeamRequests(1L);
        }

        @Test
        @DisplayName("200 OK - Retourne une liste vide si aucune demande en attente")
        void getPendingTeamRequests_EmptyList_Returns200() throws Exception {
            when(leaveRequestService.getPendingTeamRequests(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/manager/1/pending-requests"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("404 Not Found - Manager introuvable")
        void getPendingTeamRequests_ManagerNotFound_Returns404() throws Exception {
            when(leaveRequestService.getPendingTeamRequests(999L))
                    .thenThrow(new ResourceNotFoundException("Manager introuvable avec l'ID: 999"));

            mockMvc.perform(get("/api/v1/manager/999/pending-requests"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Manager introuvable avec l'ID: 999"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/manager/{managerId}/leave-requests/{requestId}/decision - Décision du Manager")
    class ProcessDecisionTests {

        @Test
        @DisplayName("200 OK - Approbation d'une demande de congé avec succès")
        void processDecision_Approve_Returns200() throws Exception {
            LeaveRequestResponseDTO approvedDTO = new LeaveRequestResponseDTO();
            approvedDTO.setId(10L);
            approvedDTO.setStatus(LeaveStatus.APPROUVEE);
            approvedDTO.setManagerComment("Demande approuvée, bon congé.");
            approvedDTO.setDecisionDate(LocalDate.now());

            when(leaveRequestService.processRequestDecision(eq(10L), eq(1L), any(LeaveApprovalDTO.class)))
                    .thenReturn(approvedDTO);

            mockMvc.perform(put("/api/v1/manager/1/leave-requests/10/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approvalDTO)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.status").value("APPROUVEE"))
                    .andExpect(jsonPath("$.managerComment").value("Demande approuvée, bon congé."))
                    .andExpect(jsonPath("$.decisionDate").isNotEmpty());

            verify(leaveRequestService, times(1)).processRequestDecision(eq(10L), eq(1L), any(LeaveApprovalDTO.class));
        }

        @Test
        @DisplayName("200 OK - Refus d'une demande de congé avec motif")
        void processDecision_Reject_Returns200() throws Exception {
            LeaveApprovalDTO rejectDTO = new LeaveApprovalDTO();
            rejectDTO.setStatus(LeaveStatus.REFUSEE);
            rejectDTO.setManagerComment("Période critique de livraison.");

            LeaveRequestResponseDTO rejectedDTO = new LeaveRequestResponseDTO();
            rejectedDTO.setId(10L);
            rejectedDTO.setStatus(LeaveStatus.REFUSEE);
            rejectedDTO.setManagerComment("Période critique de livraison.");

            when(leaveRequestService.processRequestDecision(eq(10L), eq(1L), any(LeaveApprovalDTO.class)))
                    .thenReturn(rejectedDTO);

            mockMvc.perform(put("/api/v1/manager/1/leave-requests/10/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rejectDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("REFUSEE"))
                    .andExpect(jsonPath("$.managerComment").value("Période critique de livraison."));
        }

        @Test
        @DisplayName("400 Bad Request - Validation @Valid échoue (statut null)")
        void processDecision_InvalidPayload_Returns400() throws Exception {
            LeaveApprovalDTO invalidDTO = new LeaveApprovalDTO();
            invalidDTO.setStatus(null); // @NotNull violé

            mockMvc.perform(put("/api/v1/manager/1/leave-requests/10/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").exists());

            verify(leaveRequestService, never()).processRequestDecision(any(), any(), any());
        }

        @Test
        @DisplayName("400 Bad Request - Demande déjà traitée ou employé hors équipe")
        void processDecision_AlreadyProcessedOrUnauthorized_Returns400() throws Exception {
            when(leaveRequestService.processRequestDecision(eq(10L), eq(1L), any(LeaveApprovalDTO.class)))
                    .thenThrow(new IllegalStateException("Cette demande a déjà été traitée."));

            mockMvc.perform(put("/api/v1/manager/1/leave-requests/10/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approvalDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cette demande a déjà été traitée."));
        }

        @Test
        @DisplayName("404 Not Found - Demande de congé introuvable")
        void processDecision_RequestNotFound_Returns404() throws Exception {
            when(leaveRequestService.processRequestDecision(eq(999L), eq(1L), any(LeaveApprovalDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Demande de congé introuvable avec l'ID: 999"));

            mockMvc.perform(put("/api/v1/manager/1/leave-requests/999/decision")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(approvalDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Demande de congé introuvable avec l'ID: 999"));
        }
    }
}