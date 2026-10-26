package com.wegrow.hr_management.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestCreateDTO {
    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    private String reason;

    @NotNull(message = "Le type de congé est obligatoire")
    private Long leaveTypeId;

    @NotNull(message = "L'ID de l'employé est obligatoire")
    private Long employeeId;
}