package com.wegrow.dto.request;

import com.wegrow.model.LeaveStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LeaveApprovalDTO {
    @NotNull(message = "Le statut est obligatoire")
    private LeaveStatus status; 
    private String managerComment;
}