package com.wegrow.hr_management.service;

import com.wegrow.hr_management.dto.request.EmployeeRequestDTO;
import com.wegrow.hr_management.dto.response.EmployeeResponseDTO;
import com.wegrow.hr_management.exception.ResourceAlreadyExistsException;
import com.wegrow.hr_management.mapper.EmployeeMapper;
import com.wegrow.hr_management.model.Employee;
import com.wegrow.hr_management.repository.DepartmentRepository;
import com.wegrow.hr_management.repository.EmployeeRepository;
import com.wegrow.hr_management.repository.LeaveRequestRepository;
import com.wegrow.hr_management.repository.LeaveTypeRepository;
import com.wegrow.hr_management.service.impl.EmployeeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest{

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private LeaveTypeRepository leaveTypeRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private EmployeeMapper employeeMapper;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private EmployeeRequestDTO requestDTO;
    private Employee employee;
    private EmployeeResponseDTO responseDTO;

    @BeforeEach
    void setUp(){
        requestDTO = new EmployeeRequestDTO();
        requestDTO.setMatricule("EMP001");
        requestDTO.setEmail("test@wegrow.com");
        requestDTO.setFirstName("Youssef");
        requestDTO.setLastName("Bennani");

        employee = new Employee();
        employee.setId(1L);
        employee.setMatricule("EMP001");
        employee.setEmail("test@wegrow.com");

        responseDTO = new EmployeeResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setMatricule("EMP001");
    }

    @Nested
    @DisplayName("Règle : Unicité du Matricule et de l'Email")
    class UnicityRules{

        @Test
        @DisplayName("Succès - Création d'un employé avec matricule et email uniques")
        void createEmployee_Success(){
            when(employeeRepository.findByMatricule("EMP001")).thenReturn(Optional.empty());
            when(employeeRepository.findByEmail("test@wegrow.com")).thenReturn(Optional.empty());
            when(employeeMapper.toEntity(requestDTO)).thenReturn(employee);
            when(employeeRepository.save(any(Employee.class))).thenReturn(employee);
            when(employeeMapper.toDto(employee)).thenReturn(responseDTO);

            EmployeeResponseDTO result = employeeService.create(requestDTO);

            assertNotNull(result);
            assertEquals("EMP001", result.getMatricule());
            verify(employeeRepository).save(any(Employee.class));
        }

        @Test
        @DisplayName("Erreur - Matricule déjà existant (ResourceAlreadyExistsException)")
        void createEmployee_DuplicateMatricule_ThrowsException(){
            when(employeeRepository.findByMatricule("EMP001")).thenReturn(Optional.of(employee));

            assertThrows(ResourceAlreadyExistsException.class, () -> employeeService.create(requestDTO));
            verify(employeeRepository, never()).save(any(Employee.class));
        }

        @Test
        @DisplayName("Erreur - Email déjà existant (ResourceAlreadyExistsException)")
        void createEmployee_DuplicateEmail_ThrowsException(){
            when(employeeRepository.findByMatricule("EMP001")).thenReturn(Optional.empty());
            when(employeeRepository.findByEmail("test@wegrow.com")).thenReturn(Optional.of(employee));

            assertThrows(ResourceAlreadyExistsException.class, () -> employeeService.create(requestDTO));
            verify(employeeRepository, never()).save(any(Employee.class));
        }
    }

    @Nested
    @DisplayName("Règle : Interdiction d'auto-management")
    class SelfManagementRules{

        @Test
        @DisplayName("Succès - Modification d'un employé avec un manager distinct")
        void updateEmployee_DifferentManager_Success(){
            Long employeeId = 2L;
            requestDTO.setManagerId(1L);

            Employee empToUpdate = new Employee();
            empToUpdate.setId(employeeId);

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(empToUpdate));
            when(employeeRepository.findByMatricule(any())).thenReturn(Optional.empty());
            when(employeeRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(employeeRepository.save(any())).thenReturn(empToUpdate);
            when(employeeMapper.toDto(any())).thenReturn(responseDTO);

            assertDoesNotThrow(() -> employeeService.update(employeeId, requestDTO));
        }

        @Test
        @DisplayName("Erreur - Un employé ne peut pas être son propre manager (IllegalArgumentException)")
        void updateEmployee_SelfManager_ThrowsException(){
            Long employeeId = 1L;
            requestDTO.setManagerId(1L);

            when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));

            assertThrows(IllegalArgumentException.class, () -> employeeService.update(employeeId, requestDTO));
            verify(employeeRepository, never()).save(any());
        }
    }
}