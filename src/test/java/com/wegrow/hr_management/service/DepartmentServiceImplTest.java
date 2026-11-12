package com.wegrow.hr_management.service;

import com.wegrow.hr_management.dto.request.DepartmentRequestDTO;
import com.wegrow.hr_management.dto.response.DepartmentResponseDTO;
import com.wegrow.hr_management.exception.ResourceAlreadyExistsException;
import com.wegrow.hr_management.exception.ResourceNotFoundException;
import com.wegrow.hr_management.mapper.DepartmentMapper;
import com.wegrow.hr_management.model.Department;
import com.wegrow.hr_management.model.Employee;
import com.wegrow.hr_management.repository.DepartmentRepository;
import com.wegrow.hr_management.service.impl.DepartmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private DepartmentMapper departmentMapper;

    @InjectMocks
    private DepartmentServiceImpl departmentService;

    private Department department;
    private DepartmentRequestDTO requestDTO;
    private DepartmentResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        department = new Department();
        department.setId(1L);
        department.setName("Ingénierie & IT");
        department.setDescription("Développement logiciel et infrastructure");

        requestDTO = new DepartmentRequestDTO();
        requestDTO.setName("Ingénierie & IT");
        requestDTO.setDescription("Développement logiciel et infrastructure");

        responseDTO = new DepartmentResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("Ingénierie & IT");
        responseDTO.setDescription("Développement logiciel et infrastructure");
    }

    @Nested
    @DisplayName("Consultation des Départements")
    class ReadOperations {

        @Test
        @DisplayName("Succès - Lister les départements avec pagination")
        void findAll_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Department> departmentPage = new PageImpl<>(List.of(department));

            when(departmentRepository.findAll(pageable)).thenReturn(departmentPage);
            when(departmentMapper.toDto(department)).thenReturn(responseDTO);

            Page<DepartmentResponseDTO> result = departmentService.findAll(pageable);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals("Ingénierie & IT", result.getContent().get(0).getName());
        }

        @Test
        @DisplayName("Succès - Récupérer un département par ID")
        void findById_Success() {
            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(departmentMapper.toDto(department)).thenReturn(responseDTO);

            DepartmentResponseDTO result = departmentService.findById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Erreur - Département introuvable par ID (ResourceNotFoundException)")
        void findById_NotFound_ThrowsException() {
            when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> departmentService.findById(99L));
        }
    }

    @Nested
    @DisplayName("Création & Unicité du Nom")
    class CreateOperations {

        @Test
        @DisplayName("Succès - Créer un département avec un nom unique")
        void create_Success() {
            when(departmentRepository.findByName("Ingénierie & IT")).thenReturn(Optional.empty());
            when(departmentMapper.toEntity(requestDTO)).thenReturn(department);
            when(departmentRepository.save(department)).thenReturn(department);
            when(departmentMapper.toDto(department)).thenReturn(responseDTO);

            DepartmentResponseDTO result = departmentService.create(requestDTO);

            assertNotNull(result);
            assertEquals("Ingénierie & IT", result.getName());
            verify(departmentRepository).save(department);
        }

        @Test
        @DisplayName("Erreur - Nom de département déjà existant (ResourceAlreadyExistsException)")
        void create_DuplicateName_ThrowsException() {
            when(departmentRepository.findByName("Ingénierie & IT")).thenReturn(Optional.of(department));

            assertThrows(ResourceAlreadyExistsException.class, () -> departmentService.create(requestDTO));
            verify(departmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Modification d'un Département")
    class UpdateOperations {

        @Test
        @DisplayName("Succès - Mettre à jour un département existant")
        void update_Success() {
            DepartmentRequestDTO updateDto = new DepartmentRequestDTO();
            updateDto.setName("Cloud & DevOps");
            updateDto.setDescription("Infrastructure Cloud");

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(departmentRepository.findByName("Cloud & DevOps")).thenReturn(Optional.empty());
            when(departmentRepository.save(department)).thenReturn(department);
            when(departmentMapper.toDto(department)).thenReturn(responseDTO);

            DepartmentResponseDTO result = departmentService.update(1L, updateDto);

            assertNotNull(result);
            verify(departmentRepository).save(department);
        }

        @Test
        @DisplayName("Erreur - Nom déjà utilisé par un autre département (ResourceAlreadyExistsException)")
        void update_DuplicateNameOnAnotherDepartment_ThrowsException() {
            Department anotherDept = new Department();
            anotherDept.setId(2L);
            anotherDept.setName("Cloud & DevOps");

            DepartmentRequestDTO updateDto = new DepartmentRequestDTO();
            updateDto.setName("Cloud & DevOps");

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));
            when(departmentRepository.findByName("Cloud & DevOps")).thenReturn(Optional.of(anotherDept));

            assertThrows(ResourceAlreadyExistsException.class, () -> departmentService.update(1L, updateDto));
            verify(departmentRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Suppression d'un Département")
    class DeleteOperations {

        @Test
        @DisplayName("Succès - Supprimer un département vide (sans employés)")
        void delete_EmptyDepartment_Success() {
            department.setEmployees(List.of());

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

            assertDoesNotThrow(() -> departmentService.delete(1L));
            verify(departmentRepository).delete(department);
        }

        @Test
        @DisplayName("Erreur - Interdiction de supprimer un département contenant des employés")
        void delete_DepartmentWithEmployees_ThrowsException() {
            Employee employee = new Employee();
            employee.setId(10L);
            department.setEmployees(List.of(employee)); 

            when(departmentRepository.findById(1L)).thenReturn(Optional.of(department));

            assertThrows(IllegalStateException.class, () -> departmentService.delete(1L));
            verify(departmentRepository, never()).delete(any());
        }
    }
}