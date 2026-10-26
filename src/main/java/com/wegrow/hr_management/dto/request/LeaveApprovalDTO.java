package com.wegrow.hr_management.dto.request;

import com.wegrow.hr_management.model.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveApprovalDTO {
    @NotNull(message = "Le statut est obligatoire")
    private LeaveStatus status; 
    private String managerComment;
}