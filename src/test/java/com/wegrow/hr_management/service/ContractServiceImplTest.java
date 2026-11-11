package com.wegrow.hr_management.service;

import com.wegrow.hr_management.dto.request.ContractRequestDTO;
import com.wegrow.hr_management.dto.response.ContractResponseDTO;
import com.wegrow.hr_management.mapper.ContractMapper;
import com.wegrow.hr_management.model.Contract;
import com.wegrow.hr_management.model.ContractType;
import com.wegrow.hr_management.model.Employee;
import com.wegrow.hr_management.repository.ContractRepository;
import com.wegrow.hr_management.repository.EmployeeRepository;
import com.wegrow.hr_management.service.impl.ContractServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractServiceImplTest{

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractServiceImpl contractService;

    private ContractRequestDTO requestDTO;
    private Employee employee;
    private Contract activeContract;

    @BeforeEach
    void setUp(){
        requestDTO = new ContractRequestDTO();
        requestDTO.setType(ContractType.CDI);
        requestDTO.setStartDate(LocalDate.now());
        requestDTO.setGrossSalary(new BigDecimal("12000.00"));
        requestDTO.setEmployeeId(1L);

        employee = new Employee();
        employee.setId(1L);

        activeContract = new Contract();
        activeContract.setId(10L);
        activeContract.setType(ContractType.CDI);
        activeContract.setStartDate(LocalDate.now().minusMonths(6));
        activeContract.setEndDate(null); // En cours
    }

    @Test
    @DisplayName("Succès - Créer un contrat pour un employé sans contrat actif")
    void createContract_Success(){
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(contractMapper.toEntity(requestDTO)).thenReturn(new Contract());
        when(contractRepository.save(any(Contract.class))).thenReturn(activeContract);
        when(contractMapper.toDto(any())).thenReturn(new ContractResponseDTO());

        ContractResponseDTO result = contractService.createContract(requestDTO);

        assertNotNull(result);
        verify(contractRepository).save(any(Contract.class));
    }

    @Test
    @DisplayName("Erreur - Empêcher la création si un contrat actif existe déjà (IllegalStateException)")
    void createContract_AlreadyHasActiveContract_ThrowsException(){
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.findByEmployeeId(1L)).thenReturn(List.of(activeContract));

        assertThrows(IllegalStateException.class, () -> contractService.createContract(requestDTO));
        verify(contractRepository, never()).save(any());
    }
}