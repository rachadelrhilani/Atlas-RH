package com.wegrow.dto.response;

import com.wegrow.model.ContractType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractResponseDTO {
    private Long id;
    private ContractType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal grossSalary;
    private Long employeeId;
    private String employeeFullName;
}