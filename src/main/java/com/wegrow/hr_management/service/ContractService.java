package com.wegrow.hr_management.service;

import com.wegrow.hr_management.dto.request.ContractRequestDTO;
import com.wegrow.hr_management.dto.response.ContractResponseDTO;

import java.util.List;

public interface ContractService {
    ContractResponseDTO createContract(ContractRequestDTO dto);
    ContractResponseDTO terminateContract(Long contractId);
    List<ContractResponseDTO> getEmployeeContractHistory(Long employeeId);
}