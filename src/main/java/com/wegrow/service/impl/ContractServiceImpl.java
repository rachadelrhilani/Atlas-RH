package com.wegrow.service.impl;

import com.wegrow.dto.request.ContractRequestDTO;
import com.wegrow.dto.response.ContractResponseDTO;
import com.wegrow.exception.ResourceNotFoundException;
import com.wegrow.mapper.ContractMapper;
import com.wegrow.model.Contract;
import com.wegrow.model.ContractType;
import com.wegrow.model.Employee;
import com.wegrow.repository.ContractRepository;
import com.wegrow.repository.EmployeeRepository;
import com.wegrow.service.ContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractMapper contractMapper;

    public ContractServiceImpl(ContractRepository contractRepository,
                               EmployeeRepository employeeRepository,
                               ContractMapper contractMapper) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.contractMapper = contractMapper;
    }

    @Override
    public ContractResponseDTO createContract(ContractRequestDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employé non trouvé avec l'ID : " + dto.getEmployeeId()));

        List<Contract> activeContracts = contractRepository.findByEmployeeId(employee.getId()).stream()
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(LocalDate.now()))
                .collect(Collectors.toList());

        if (!activeContracts.isEmpty()) {
            throw new IllegalStateException("L'employé possède déjà un contrat en cours. Veuillez le terminer avant d'en créer un nouveau.");
        }

        if ((dto.getType() == ContractType.CDD || dto.getType() == ContractType.STAGE) && dto.getEndDate() == null) {
            throw new IllegalArgumentException("Un contrat de type " + dto.getType() + " doit impérativement avoir une date de fin.");
        }

        Contract contract = contractMapper.toEntity(dto);
        contract.setEmployee(employee);

        return contractMapper.toDto(contractRepository.save(contract));
    }

    @Override
    public ContractResponseDTO terminateContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Contrat non trouvé avec l'ID : " + contractId));

        if (contract.getEndDate() != null && contract.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Ce contrat est déjà terminé.");
        }

        contract.setEndDate(LocalDate.now());
        return contractMapper.toDto(contractRepository.save(contract));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponseDTO> getEmployeeContractHistory(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employé non trouvé avec l'ID : " + employeeId);
        }
        return contractRepository.findByEmployeeId(employeeId).stream()
                .map(contractMapper::toDto)
                .collect(Collectors.toList());
    }
}