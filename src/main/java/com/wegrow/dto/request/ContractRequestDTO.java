package com.wegrow.dto.request;

import com.wegrow.model.ContractType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ContractRequestDTO {
    @NotNull(message = "Le type de contrat est obligatoire")
    private ContractType type;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Le salaire brut est obligatoire")
    @Positive(message = "Le salaire brut doit être supérieur à zéro")
    private BigDecimal grossSalary;

    @NotNull(message = "L'ID de l'employé est obligatoire")
    private Long employeeId;
}