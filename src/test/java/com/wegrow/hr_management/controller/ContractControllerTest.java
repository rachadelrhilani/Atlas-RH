package com.wegrow.hr_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegrow.hr_management.dto.request.ContractRequestDTO;
import com.wegrow.hr_management.dto.response.ContractResponseDTO;
import com.wegrow.hr_management.exception.ResourceNotFoundException;
import com.wegrow.hr_management.model.ContractType;
import com.wegrow.hr_management.service.ContractService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContractController.class)
class ContractControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ContractService contractService;

    private ContractRequestDTO validRequestDTO;
    private ContractResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        validRequestDTO = new ContractRequestDTO();
        validRequestDTO.setType(ContractType.CDI);
        validRequestDTO.setStartDate(LocalDate.of(2026, 11, 1));
        validRequestDTO.setEndDate(null);
        validRequestDTO.setGrossSalary(new BigDecimal("15000.00"));
        validRequestDTO.setEmployeeId(1L);

        responseDTO = new ContractResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setType(ContractType.CDI);
        responseDTO.setStartDate(LocalDate.of(2026, 11, 1));
        responseDTO.setEndDate(null);
        responseDTO.setGrossSalary(new BigDecimal("15000.00"));
        responseDTO.setEmployeeId(1L);
    }

    @Nested
    @DisplayName("POST /api/v1/contracts - Création d'un contrat")
    class CreateContractTests {

        @Test
        @DisplayName("201 Created - Contrat créé avec succès et format JSON vérifié")
        void createContract_Success_Returns201() throws Exception {
            when(contractService.createContract(any(ContractRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(post("/api/v1/contracts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.type").value("CDI"))
                    .andExpect(jsonPath("$.startDate").value("2026-11-01"))
                    .andExpect(jsonPath("$.grossSalary").value(15000.00))
                    .andExpect(jsonPath("$.employeeId").value(1L));

            verify(contractService, times(1)).createContract(any(ContractRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validation @Valid échoue (champs obligatoires manquants ou salaire négatif)")
        void createContract_InvalidPayload_Returns400() throws Exception {
            ContractRequestDTO invalidDTO = new ContractRequestDTO();
            invalidDTO.setType(null); // @NotNull violé
            invalidDTO.setGrossSalary(new BigDecimal("-500.00")); // @Positive violé

            mockMvc.perform(post("/api/v1/contracts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest());

            verify(contractService, never()).createContract(any());
        }

        @Test
        @DisplayName("400 Bad Request - Règle métier violée (contrat actif déjà existant ou date de fin manquante pour CDD)")
        void createContract_BusinessRuleViolation_Returns400() throws Exception {
            when(contractService.createContract(any(ContractRequestDTO.class)))
                    .thenThrow(new IllegalStateException("L'employé possède déjà un contrat actif en cours."));

            mockMvc.perform(post("/api/v1/contracts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 Not Found - Employé introuvable")
        void createContract_EmployeeNotFound_Returns404() throws Exception {
            when(contractService.createContract(any(ContractRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Employé non trouvé avec l'ID : 99"));

            mockMvc.perform(post("/api/v1/contracts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/contracts/{id}/terminate - Clôture d'un contrat")
    class TerminateContractTests {

        @Test
        @DisplayName("200 OK - Contrat clôturé avec succès")
        void terminateContract_Success_Returns200() throws Exception {
            LocalDate today = LocalDate.now();
            responseDTO.setEndDate(today);

            when(contractService.terminateContract(10L)).thenReturn(responseDTO);

            mockMvc.perform(put("/api/v1/contracts/10/terminate"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.endDate").value(today.toString()));

            verify(contractService, times(1)).terminateContract(10L);
        }

        @Test
        @DisplayName("400 Bad Request - Contrat déjà clôturé ou arrivé à terme")
        void terminateContract_AlreadyTerminated_Returns400() throws Exception {
            when(contractService.terminateContract(10L))
                    .thenThrow(new IllegalStateException("Ce contrat est déjà clôturé."));

            mockMvc.perform(put("/api/v1/contracts/10/terminate"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 Not Found - Contrat introuvable")
        void terminateContract_NotFound_Returns404() throws Exception {
            when(contractService.terminateContract(999L))
                    .thenThrow(new ResourceNotFoundException("Contrat non trouvé avec l'ID : 999"));

            mockMvc.perform(put("/api/v1/contracts/999/terminate"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/contracts/employee/{employeeId} - Historique des contrats")
    class GetContractHistoryTests {

        @Test
        @DisplayName("200 OK - Retourne la liste JSON de l'historique des contrats")
        void getEmployeeContractHistory_Success_Returns200() throws Exception {
            ContractResponseDTO pastStage = new ContractResponseDTO();
            pastStage.setId(5L);
            pastStage.setType(ContractType.STAGE);
            pastStage.setStartDate(LocalDate.of(2026, 3, 1));
            pastStage.setEndDate(LocalDate.of(2026, 8, 31));
            pastStage.setGrossSalary(new BigDecimal("4500.00"));
            pastStage.setEmployeeId(1L);

            when(contractService.getEmployeeContractHistory(1L)).thenReturn(List.of(pastStage, responseDTO));

            mockMvc.perform(get("/api/v1/contracts/employee/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(5L))
                    .andExpect(jsonPath("$[0].type").value("STAGE"))
                    .andExpect(jsonPath("$[1].id").value(10L))
                    .andExpect(jsonPath("$[1].type").value("CDI"));

            verify(contractService, times(1)).getEmployeeContractHistory(1L);
        }

        @Test
        @DisplayName("404 Not Found - Employé introuvable lors de la consultation de l'historique")
        void getEmployeeContractHistory_EmployeeNotFound_Returns404() throws Exception {
            when(contractService.getEmployeeContractHistory(999L))
                    .thenThrow(new ResourceNotFoundException("Employé non trouvé avec l'ID : 999"));

            mockMvc.perform(get("/api/v1/contracts/employee/999"))
                    .andExpect(status().isNotFound());
        }
    }
}