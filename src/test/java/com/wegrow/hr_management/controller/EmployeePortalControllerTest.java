package com.wegrow.hr_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegrow.hr_management.dto.request.LeaveRequestCreateDTO;
import com.wegrow.hr_management.dto.response.LeaveBalanceResponseDTO;
import com.wegrow.hr_management.dto.response.LeaveRequestResponseDTO;
import com.wegrow.hr_management.dto.response.SeniorityResponseDTO;
import com.wegrow.hr_management.exception.ResourceNotFoundException;
import com.wegrow.hr_management.model.LeaveStatus;
import com.wegrow.hr_management.service.EmployeeService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeePortalController.class)
class EmployeePortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveRequestService leaveRequestService;

    @MockBean
    private EmployeeService employeeService;

    private LeaveRequestCreateDTO validCreateDTO;
    private LeaveRequestResponseDTO leaveResponseDTO;

    @BeforeEach
    void setUp() {
        validCreateDTO = new LeaveRequestCreateDTO();
        validCreateDTO.setEmployeeId(2L);
        validCreateDTO.setLeaveTypeId(1L);
        validCreateDTO.setStartDate(LocalDate.now().plusDays(5));
        validCreateDTO.setEndDate(LocalDate.now().plusDays(9));
        validCreateDTO.setReason("Congés annuels");

        leaveResponseDTO = new LeaveRequestResponseDTO();
        leaveResponseDTO.setId(10L);
        leaveResponseDTO.setEmployeeId(2L);
        leaveResponseDTO.setEmployeeFullName("Rachad El Rhilani");
        leaveResponseDTO.setLeaveTypeName("Congé Payé");
        leaveResponseDTO.setStartDate(validCreateDTO.getStartDate());
        leaveResponseDTO.setEndDate(validCreateDTO.getEndDate());
        leaveResponseDTO.setWorkingDays(5);
        leaveResponseDTO.setStatus(LeaveStatus.EN_ATTENTE);
    }

    @Nested
    @DisplayName("POST /api/v1/employee-portal/leave-requests - Soumettre une demande (US5)")
    class CreateLeaveRequestTests {

        @Test
        @DisplayName("201 Created - Demande créée avec succès et format JSON vérifié")
        void createRequest_Success_Returns201() throws Exception {
            when(leaveRequestService.createRequest(any(LeaveRequestCreateDTO.class))).thenReturn(leaveResponseDTO);

            mockMvc.perform(post("/api/v1/employee-portal/leave-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.employeeId").value(2L))
                    .andExpect(jsonPath("$.leaveTypeName").value("Congé Payé"))
                    .andExpect(jsonPath("$.workingDays").value(5))
                    .andExpect(jsonPath("$.status").value("EN_ATTENTE"));

            verify(leaveRequestService, times(1)).createRequest(any(LeaveRequestCreateDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validation @Valid échoue (date dans le passé ou champs obligatoires null)")
        void createRequest_InvalidPayload_Returns400() throws Exception {
            LeaveRequestCreateDTO invalidDTO = new LeaveRequestCreateDTO();
            invalidDTO.setStartDate(LocalDate.now().minusDays(3)); // @FutureOrPresent violé
            invalidDTO.setEndDate(null); // @NotNull violé

            mockMvc.perform(post("/api/v1/employee-portal/leave-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.startDate").exists())
                    .andExpect(jsonPath("$.endDate").exists());

            verify(leaveRequestService, never()).createRequest(any());
        }

        @Test
        @DisplayName("400 Bad Request - Solde insuffisant ou chevauchement de dates")
        void createRequest_BusinessRuleViolation_Returns400() throws Exception {
            when(leaveRequestService.createRequest(any(LeaveRequestCreateDTO.class)))
                    .thenThrow(new IllegalArgumentException("Solde insuffisant pour ce type de congé."));

            mockMvc.perform(post("/api/v1/employee-portal/leave-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Solde insuffisant pour ce type de congé."));
        }

        @Test
        @DisplayName("404 Not Found - Employé ou Type de congé introuvable")
        void createRequest_NotFound_Returns404() throws Exception {
            when(leaveRequestService.createRequest(any(LeaveRequestCreateDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Type de congé introuvable avec l'ID: 99"));

            mockMvc.perform(post("/api/v1/employee-portal/leave-requests")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateDTO)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Type de congé introuvable avec l'ID: 99"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employee-portal/{employeeId}/leave-requests - Historique des demandes (US5)")
    class GetEmployeeRequestsTests {

        @Test
        @DisplayName("200 OK - Récupérer toutes les demandes sans filtre")
        void getEmployeeRequests_WithoutFilter_Returns200() throws Exception {
            when(leaveRequestService.getEmployeeRequests(2L, null)).thenReturn(List.of(leaveResponseDTO));

            mockMvc.perform(get("/api/v1/employee-portal/2/leave-requests"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(10L))
                    .andExpect(jsonPath("$[0].status").value("EN_ATTENTE"));

            verify(leaveRequestService, times(1)).getEmployeeRequests(2L, null);
        }

        @Test
        @DisplayName("200 OK - Filtrer les demandes par statut (EN_ATTENTE)")
        void getEmployeeRequests_WithStatusFilter_Returns200() throws Exception {
            when(leaveRequestService.getEmployeeRequests(2L, LeaveStatus.EN_ATTENTE))
                    .thenReturn(List.of(leaveResponseDTO));

            mockMvc.perform(get("/api/v1/employee-portal/2/leave-requests")
                            .param("status", "EN_ATTENTE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("EN_ATTENTE"));

            verify(leaveRequestService, times(1)).getEmployeeRequests(2L, LeaveStatus.EN_ATTENTE);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/employee-portal/{employeeId}/leave-requests/{requestId}/cancel - Annulation (US5)")
    class CancelLeaveRequestTests {

        @Test
        @DisplayName("200 OK - Demande annulée avec succès")
        void cancelRequest_Success_Returns200() throws Exception {
            leaveResponseDTO.setStatus(LeaveStatus.ANNULEE);
            when(leaveRequestService.cancelRequest(10L, 2L)).thenReturn(leaveResponseDTO);

            mockMvc.perform(put("/api/v1/employee-portal/2/leave-requests/10/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.status").value("ANNULEE"));

            verify(leaveRequestService, times(1)).cancelRequest(10L, 2L);
        }

        @Test
        @DisplayName("400 Bad Request - Demande déjà approuvée ou refusée (annulation interdite)")
        void cancelRequest_AlreadyProcessed_Returns400() throws Exception {
            when(leaveRequestService.cancelRequest(10L, 2L))
                    .thenThrow(new IllegalStateException("Seules les demandes en attente peuvent être annulées."));

            mockMvc.perform(put("/api/v1/employee-portal/2/leave-requests/10/cancel"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Seules les demandes en attente peuvent être annulées."));
        }

        @Test
        @DisplayName("404 Not Found - Demande introuvable")
        void cancelRequest_NotFound_Returns404() throws Exception {
            when(leaveRequestService.cancelRequest(999L, 2L))
                    .thenThrow(new ResourceNotFoundException("Demande de congé introuvable."));

            mockMvc.perform(put("/api/v1/employee-portal/2/leave-requests/999/cancel"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employee-portal/{employeeId}/balances - Solde de congés (US6)")
    class GetLeaveBalancesTests {

        @Test
        @DisplayName("200 OK - Retourne le solde de congés restant ventilé par type")
        void getLeaveBalances_Success_Returns200() throws Exception {
            LeaveBalanceResponseDTO balance = new LeaveBalanceResponseDTO();
            balance.setLeaveTypeId(1L);
            balance.setLeaveTypeName("Congé Payé");
            balance.setTotalDaysAllowed(22);
            balance.setDaysUsed(5);
            balance.setRemainingDays(17);

            when(employeeService.getLeaveBalances(2L)).thenReturn(List.of(balance));

            mockMvc.perform(get("/api/v1/employee-portal/2/balances"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].leaveTypeName").value("Congé Payé"))
                    .andExpect(jsonPath("$[0].totalDaysAllowed").value(22))
                    .andExpect(jsonPath("$[0].daysUsed").value(5))
                    .andExpect(jsonPath("$[0].remainingDays").value(17));

            verify(employeeService, times(1)).getLeaveBalances(2L);
        }

        @Test
        @DisplayName("404 Not Found - Employé introuvable")
        void getLeaveBalances_EmployeeNotFound_Returns404() throws Exception {
            when(employeeService.getLeaveBalances(999L))
                    .thenThrow(new ResourceNotFoundException("Employé non trouvé avec l'ID : 999"));

            mockMvc.perform(get("/api/v1/employee-portal/999/balances"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employee-portal/{employeeId}/seniority - Ancienneté (US6)")
    class GetSeniorityTests {

        @Test
        @DisplayName("200 OK - Retourne l'ancienneté calculée en années et mois")
        void getSeniority_Success_Returns200() throws Exception {
            SeniorityResponseDTO seniority = new SeniorityResponseDTO();
            seniority.setYears(1);
            seniority.setMonths(10);
            seniority.setFormattedSeniority("1 an(s) et 10 mois");

            when(employeeService.getSeniority(2L)).thenReturn(seniority);

            mockMvc.perform(get("/api/v1/employee-portal/2/seniority"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.years").value(1))
                    .andExpect(jsonPath("$.months").value(10))
                    .andExpect(jsonPath("$.formattedSeniority").value("1 an(s) et 10 mois"));

            verify(employeeService, times(1)).getSeniority(2L);
        }

        @Test
        @DisplayName("404 Not Found - Employé introuvable")
        void getSeniority_EmployeeNotFound_Returns404() throws Exception {
            when(employeeService.getSeniority(999L))
                    .thenThrow(new ResourceNotFoundException("Employé non trouvé avec l'ID : 999"));

            mockMvc.perform(get("/api/v1/employee-portal/999/seniority"))
                    .andExpect(status().isNotFound());
        }
    }
}