package com.wegrow.hr_management.dto.response;

import com.wegrow.hr_management.model.LeaveStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestResponseDTO {
    private Long id;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer workingDays;
    private String reason;
    private LeaveStatus status;
    private String managerComment;
    private LocalDate decisionDate;
    private Long employeeId;
    private String employeeFullName;
    private String leaveTypeName;
}