package com.wegrow.hr_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegrow.hr_management.dto.request.DepartmentRequestDTO;
import com.wegrow.hr_management.dto.response.DepartmentResponseDTO;
import com.wegrow.hr_management.exception.ResourceAlreadyExistsException;
import com.wegrow.hr_management.exception.ResourceNotFoundException;
import com.wegrow.hr_management.service.DepartmentService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    private DepartmentRequestDTO validRequestDTO;
    private DepartmentResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        validRequestDTO = new DepartmentRequestDTO();
        validRequestDTO.setName("Ingénierie & IT");
        validRequestDTO.setDescription("Développement logiciel et infrastructure");

        responseDTO = new DepartmentResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("Ingénierie & IT");
        responseDTO.setDescription("Développement logiciel et infrastructure");
    }

    @Nested
    @DisplayName("GET /api/v1/departments - Lister les départements")
    class GetAllDepartmentsTests {

        @Test
        @DisplayName("200 OK - Retourne une page JSON de départements")
        void getAll_Success_Returns200() throws Exception {
            Page<DepartmentResponseDTO> page = new PageImpl<>(List.of(responseDTO), PageRequest.of(0, 10), 1);
            when(departmentService.findAll(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/api/v1/departments")
                            .param("page", "0")
                            .param("size", "10")
                            .param("sort", "name,asc"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(1L))
                    .andExpect(jsonPath("$.content[0].name").value("Ingénierie & IT"))
                    .andExpect(jsonPath("$.content[0].description").value("Développement logiciel et infrastructure"))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(departmentService, times(1)).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/departments/{id} - Consulter un département")
    class GetDepartmentByIdTests {

        @Test
        @DisplayName("200 OK - Département trouvé avec format JSON valide")
        void getById_Success_Returns200() throws Exception {
            when(departmentService.findById(1L)).thenReturn(responseDTO);

            mockMvc.perform(get("/api/v1/departments/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Ingénierie & IT"));

            verify(departmentService, times(1)).findById(1L);
        }

        @Test
        @DisplayName("404 Not Found - Département inexistant")
        void getById_NotFound_Returns404() throws Exception {
            when(departmentService.findById(99L))
                    .thenThrow(new ResourceNotFoundException("Département non trouvé avec l'ID : 99"));

            mockMvc.perform(get("/api/v1/departments/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Département non trouvé avec l'ID : 99"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/departments - Créer un département")
    class CreateDepartmentTests {

        @Test
        @DisplayName("201 Created - Département créé avec succès")
        void create_Success_Returns201() throws Exception {
            when(departmentService.create(any(DepartmentRequestDTO.class))).thenReturn(responseDTO);

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Ingénierie & IT"));

            verify(departmentService, times(1)).create(any(DepartmentRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Échec de validation @Valid (nom vide ou nul)")
        void create_InvalidPayload_Returns400() throws Exception {
            DepartmentRequestDTO invalidDTO = new DepartmentRequestDTO();
            invalidDTO.setName(""); // @NotBlank violé

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.name").exists());

            verify(departmentService, never()).create(any());
        }

        @Test
        @DisplayName("409 Conflict - Nom de département déjà existant")
        void create_DuplicateName_Returns409() throws Exception {
            when(departmentService.create(any(DepartmentRequestDTO.class)))
                    .thenThrow(new ResourceAlreadyExistsException("Le département 'Ingénierie & IT' existe déjà."));

            mockMvc.perform(post("/api/v1/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.message").value("Le département 'Ingénierie & IT' existe déjà."));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/departments/{id} - Modifier un département")
    class UpdateDepartmentTests {

        @Test
        @DisplayName("200 OK - Département mis à jour avec succès")
        void update_Success_Returns200() throws Exception {
            DepartmentResponseDTO updatedDTO = new DepartmentResponseDTO();
            updatedDTO.setId(1L);
            updatedDTO.setName("Cloud & DevOps");
            updatedDTO.setDescription("Infrastructure Cloud");

            when(departmentService.update(eq(1L), any(DepartmentRequestDTO.class))).thenReturn(updatedDTO);

            mockMvc.perform(put("/api/v1/departments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.name").value("Cloud & DevOps"));

            verify(departmentService, times(1)).update(eq(1L), any(DepartmentRequestDTO.class));
        }

        @Test
        @DisplayName("400 Bad Request - Validation @Valid échoue lors de la mise à jour")
        void update_InvalidPayload_Returns400() throws Exception {
            DepartmentRequestDTO invalidDTO = new DepartmentRequestDTO();
            invalidDTO.setName("   "); // @NotBlank violé

            mockMvc.perform(put("/api/v1/departments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDTO)))
                    .andExpect(status().isBadRequest());

            verify(departmentService, never()).update(any(), any());
        }

        @Test
        @DisplayName("404 Not Found - Département à modifier introuvable")
        void update_NotFound_Returns404() throws Exception {
            when(departmentService.update(eq(99L), any(DepartmentRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Département non trouvé avec l'ID : 99"));

            mockMvc.perform(put("/api/v1/departments/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("409 Conflict - Nom déjà utilisé par un autre département")
        void update_DuplicateName_Returns409() throws Exception {
            when(departmentService.update(eq(1L), any(DepartmentRequestDTO.class)))
                    .thenThrow(new ResourceAlreadyExistsException("Ce nom de département est déjà utilisé."));

            mockMvc.perform(put("/api/v1/departments/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequestDTO)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/departments/{id} - Supprimer un département")
    class DeleteDepartmentTests {

        @Test
        @DisplayName("204 No Content - Département supprimé avec succès")
        void delete_Success_Returns204() throws Exception {
            doNothing().when(departmentService).delete(1L);

            mockMvc.perform(delete("/api/v1/departments/1"))
                    .andExpect(status().isNoContent());

            verify(departmentService, times(1)).delete(1L);
        }

        @Test
        @DisplayName("400 Bad Request - Département contient encore des employés")
        void delete_ContainsEmployees_Returns400() throws Exception {
            doThrow(new IllegalStateException("Impossible de supprimer un département contenant encore des employés."))
                    .when(departmentService).delete(1L);

            mockMvc.perform(delete("/api/v1/departments/1"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Impossible de supprimer un département contenant encore des employés."));
        }

        @Test
        @DisplayName("404 Not Found - Département à supprimer introuvable")
        void delete_NotFound_Returns404() throws Exception {
            doThrow(new ResourceNotFoundException("Département non trouvé avec l'ID : 99"))
                    .when(departmentService).delete(99L);

            mockMvc.perform(delete("/api/v1/departments/99"))
                    .andExpect(status().isNotFound());
        }
    }
}