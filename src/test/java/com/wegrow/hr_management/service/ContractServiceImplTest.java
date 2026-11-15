package com.wegrow.hr_management.service;

import com.wegrow.hr_management.dto.request.ContractRequestDTO;
import com.wegrow.hr_management.dto.response.ContractResponseDTO;
import com.wegrow.hr_management.exception.ResourceNotFoundException;
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
class ContractServiceImplTest {

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
    private ContractResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
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
        activeContract.setEndDate(null);

        responseDTO = new ContractResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setType(ContractType.CDI);
    }

    @Test
    @DisplayName("Succès - Créer un contrat CDI pour un employé sans contrat actif")
    void createContract_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(contractMapper.toEntity(requestDTO)).thenReturn(new Contract());
        when(contractRepository.save(any(Contract.class))).thenReturn(activeContract);
        when(contractMapper.toDto(any())).thenReturn(responseDTO);

        ContractResponseDTO result = contractService.createContract(requestDTO);

        assertNotNull(result);
        verify(contractRepository).save(any(Contract.class));
    }

    @Test
    @DisplayName("Succès - Créer un contrat CDD avec une date de fin valide alors que l'ancien contrat est expiré")
    void createContract_CddWithEndDateAndPreviousExpired_Success() {
        Contract expiredContract = new Contract();
        expiredContract.setId(9L);
        expiredContract.setEndDate(LocalDate.now().minusDays(5));

        requestDTO.setType(ContractType.CDD);
        requestDTO.setEndDate(LocalDate.now().plusMonths(6));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.findByEmployeeId(1L)).thenReturn(List.of(expiredContract));
        when(contractMapper.toEntity(requestDTO)).thenReturn(new Contract());
        when(contractRepository.save(any(Contract.class))).thenReturn(activeContract);
        when(contractMapper.toDto(any())).thenReturn(responseDTO);

        ContractResponseDTO result = contractService.createContract(requestDTO);

        assertNotNull(result);
        verify(contractRepository).save(any(Contract.class));
    }

    @Test
    @DisplayName("Erreur - Créer un contrat pour un employé introuvable")
    void createContract_EmployeeNotFound_ThrowsException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contractService.createContract(requestDTO));
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Erreur - Empêcher la création si un contrat actif existe déjà (endDate null ou future)")
    void createContract_AlreadyHasActiveContract_ThrowsException() {
        Contract futureEndContract = new Contract();
        futureEndContract.setEndDate(LocalDate.now().plusDays(10));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.findByEmployeeId(1L)).thenReturn(List.of(futureEndContract));

        assertThrows(IllegalStateException.class, () -> contractService.createContract(requestDTO));
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Erreur - Empêcher la création d'un CDD ou STAGE sans date de fin")
    void createContract_CddOrStageWithoutEndDate_ThrowsException() {
        requestDTO.setType(ContractType.STAGE);
        requestDTO.setEndDate(null);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(contractRepository.findByEmployeeId(1L)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class, () -> contractService.createContract(requestDTO));
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Succès - Clôturer un contrat en cours")
    void terminateContract_Success() {
        when(contractRepository.findById(10L)).thenReturn(Optional.of(activeContract));
        when(contractRepository.save(activeContract)).thenReturn(activeContract);
        when(contractMapper.toDto(activeContract)).thenReturn(responseDTO);

        ContractResponseDTO result = contractService.terminateContract(10L);

        assertNotNull(result);
        assertEquals(LocalDate.now(), activeContract.getEndDate());
        verify(contractRepository).save(activeContract);
    }

    @Test
    @DisplayName("Erreur - Clôturer un contrat introuvable")
    void terminateContract_NotFound_ThrowsException() {
        when(contractRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contractService.terminateContract(99L));
    }

    @Test
    @DisplayName("Erreur - Clôturer un contrat déjà terminé")
    void terminateContract_AlreadyTerminated_ThrowsException() {
        activeContract.setEndDate(LocalDate.now().minusDays(2));
        when(contractRepository.findById(10L)).thenReturn(Optional.of(activeContract));

        assertThrows(IllegalStateException.class, () -> contractService.terminateContract(10L));
        verify(contractRepository, never()).save(any());
    }

    @Test
    @DisplayName("Succès - Récupérer l'historique des contrats d'un employé")
    void getEmployeeContractHistory_Success() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(contractRepository.findByEmployeeId(1L)).thenReturn(List.of(activeContract));
        when(contractMapper.toDto(activeContract)).thenReturn(responseDTO);

        List<ContractResponseDTO> history = contractService.getEmployeeContractHistory(1L);

        assertEquals(1, history.size());
    }

    @Test
    @DisplayName("Erreur - Historique des contrats pour un employé inexistant")
    void getEmployeeContractHistory_EmployeeNotFound_ThrowsException() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> contractService.getEmployeeContractHistory(99L));
    }
}