package com.wegrow.hr_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegrow.hr_management.dto.request.EmployeeRequestDTO;
import com.wegrow.hr_management.dto.response.EmployeeResponseDTO;
import com.wegrow.hr_management.exception.ResourceAlreadyExistsException;
import com.wegrow.hr_management.exception.ResourceNotFoundException;
import com.wegrow.hr_management.model.Role;
import com.wegrow.hr_management.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private EmployeeRequestDTO validRequestDTO;
    private EmployeeResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        validRequestDTO = new EmployeeRequestDTO();
        validRequestDTO.setMatricule("EMP001");
        validRequestDTO.setFirstName("Rachad");
        validRequestDTO.setLastName("El Rhilani");
        validRequestDTO.setEmail("rachad.elrhilani@wegrow.com");
        validRequestDTO.setPosition("Développeur Full-Stack Java");
        validRequestDTO.setHireDate(LocalDate.of(2025, 1, 15));
        validRequestDTO.setRole(Role.EMPLOYE);
        validRequestDTO.setDepartmentId(1L);
        validRequestDTO.setManagerId(2L);

        responseDTO = new EmployeeResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setMatricule("EMP001");
        responseDTO.setFirstName("Rachad");
        responseDTO.setLastName("El Rhilani");
        responseDTO.setEmail("rachad.elrhilani@wegrow.com");
        responseDTO.setPosition("Développeur Full-Stack Java");
        responseDTO.setRole(Role.EMPLOYE);
    }

    @Nested
    @DisplayName("GET /api/v1/employees - Recherche et pagination des employés")
    class SearchEmployeesTests {

        @Test
        @DisplayName("200 OK - Retourne une page JSON filtrée par nom et département")
        void search_WithFilters_Returns200() throws Exception {
            Page<EmployeeResponseDTO> page = new PageImpl<>(List.of(responseDTO), PageRequest.of(0, 10), 1);
            when(employeeService.search(eq("Rachad"), eq(1L), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/employees")
                            .param("name", "Rachad")
                            .param("departmentId", "1")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(10L))
                    .andExpect(jsonPath("$.content[0].matricule").value("EMP001"))
                    .andExpect(jsonPath("$.content[0].firstName").value("Rachad"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(employeeService, times(1)).search(eq("Rachad"), eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("200 OK - Liste sans filtres")
        void search_WithoutFilters_Returns200() throws Exception {
            Page<EmployeeResponseDTO> page = new PageImpl<>(List.of(responseDTO));
            when(employeeService.search(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].email").value("rachad.elrhilani@wegrow.com"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employees/{id} - Consultation fiche employé")
    class GetEmployeeByIdTests {

        @Test
        @DisplayName("200 OK - Fiche employé trouvée")
        void getById_Success_Returns200() throws Exception {
            when(employeeService.findById(10L)).thenReturn(responseDTO);

            mockMvc.perform(get("/api/v1/employees/10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.matricule").value("EMP001"))
                    .andExpect(jsonPath("$.lastName").value("El Rhilani"));

            verify(employeeService, times(1)).findById(10L);
        }

        @Test
        @DisplayName("404 Not Found - Employé introuvable")
        void getById_NotFound_Returns404() throws Exception {
            when(employeeService.findById(999L))
                    .thenThrow(new ResourceNotFoundException("Employé non trouvé avec l'ID : 999"));

            mockMvc.perform(get("/api/v1/employees/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Employé non trouvé avec l'ID : 999"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/employees/manager/{managerId}/team - Équipe d'un manager")
    class GetTeamByManagerTests {

        @Test
        @DisplayName("200 OK - Liste JSON des membres de l'équipe")
        void getTeamByManager_Success_Returns200() throws Exception {
            when(employeeService.findTeamByManager(2L)).thenReturn(List.of(responseDTO));

            mockMvc.perform(get("/api/v1/employees/manager/2/team"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(10L))
                    .andExpect(jsonPath("$[0].firstName").value("Rachad"));

            verify(employeeService, times(1)).findTeamByManager(2L);
        }

        @Test
        @DisplayName("404 Not Found - Manager introuvable")
        void getTeamByManager_NotFound_Returns404() throws Exception {
            when(employeeService.findTeamByManager(999L))
                    .thenThrow(new ResourceNotFoundException("Manager non trouvé avec l'ID : 999"));

            mockMvc.perform(get("/api/v1/employees/manager/999/team"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/employees - Création d'un employé")
    class CreateEmployeeTests {

        @Test
        @DisplayName("201 Created - Employé créé avec succès")
        void create_Success_Returns201() throws Exception {
            when(employeeService.create(any(EmployeeRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.matricule").value("EMP001"))
                    .andExpect(jsonPath("$.email").value("rachad.elrhilani@wegrow.com"));

            verify(employeeService, times(1)).create(any(EmployeeRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validation @Valid échoue (email invalide ou champs obligatoires vides)")
        void create_InvalidPayload_Returns400() throws Exception {
            EmployeeRequestDTO invalidDTO = new EmployeeRequestDTO();
            invalidDTO.setMatricule(""); // @NotBlank violé
            invalidDTO.setEmail("format-email-invalide"); // @Email violé

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.matricule").exists())
                    .andExpect(jsonPath("$.email").exists());

            verify(employeeService, never()).create(any());
        }

        @Test
        @DisplayName("409 Conflict - Matricule ou e-mail déjà utilisé")
        void create_DuplicateMatriculeOrEmail_Returns409() throws Exception {
            when(employeeService.create(any(EmployeeRequestDTO.class)))
                    .thenThrow(new ResourceAlreadyExistsException("Le matricule 'EMP001' est déjà attribué."));

            mockMvc.perform(post("/api/v1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Le matricule 'EMP001' est déjà attribué."));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/employees/{id} - Modification d'un employé")
    class UpdateEmployeeTests {

        @Test
        @DisplayName("200 OK - Fiche employé mise à jour avec succès")
        void update_Success_Returns200() throws Exception {
            responseDTO.setPosition("Lead Développeur Java");
            when(employeeService.update(eq(10L), any(EmployeeRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(put("/api/v1/employees/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(10L))
                    .andExpect(jsonPath("$.position").value("Lead Développeur Java"));

            verify(employeeService, times(1)).update(eq(10L), any(EmployeeRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Tentative d'auto-management")
        void update_SelfManager_Returns400() throws Exception {
            validRequestDTO.setManagerId(10L);
            when(employeeService.update(eq(10L), any(EmployeeRequestDTO.class)))
                    .thenThrow(new IllegalArgumentException("Un employé ne peut pas être son propre manager."));

            mockMvc.perform(put("/api/v1/employees/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Un employé ne peut pas être son propre manager."));
        }

        @Test
        @DisplayName("404 Not Found - Employé, département ou manager introuvable")
        void update_ResourceNotFound_Returns404() throws Exception {
            when(employeeService.update(eq(999L), any(EmployeeRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Employé non trouvé avec l'ID : 999"));

            mockMvc.perform(put("/api/v1/employees/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("409 Conflict - Matricule ou email déjà utilisé par un autre employé")
        void update_DuplicateData_Returns409() throws Exception {
            when(employeeService.update(eq(10L), any(EmployeeRequestDTO.class)))
                    .thenThrow(new ResourceAlreadyExistsException("L'e-mail est déjà utilisé."));

            mockMvc.perform(put("/api/v1/employees/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isConflict());
        }
    }
}