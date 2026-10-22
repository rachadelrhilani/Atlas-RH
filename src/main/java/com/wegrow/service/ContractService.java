package com.wegrow.service;

import com.wegrow.dto.request.ContractRequestDTO;
import com.wegrow.dto.response.ContractResponseDTO;

import java.util.List;

public interface ContractService {
    ContractResponseDTO createContract(ContractRequestDTO dto);
    ContractResponseDTO terminateContract(Long contractId);
    List<ContractResponseDTO> getEmployeeContractHistory(Long employeeId);
}