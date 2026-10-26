package com.wegrow.hr_management.dto.request;

import com.wegrow.hr_management.model.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LeaveApprovalDTO {
    @NotNull(message = "Le statut est obligatoire (APPROUVEE ou REFUSEE)")
    private LeaveStatus status; 
    @Size(max = 500, message = "Le commentaire ne peut pas dépasser 500 caractères")
    private String managerComment;
}