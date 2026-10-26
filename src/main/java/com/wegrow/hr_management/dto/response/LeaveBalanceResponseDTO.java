package com.wegrow.hr_management.dto.response;

import lombok.Data;

@Data
public class LeaveBalanceResponseDTO {
    private Long leaveTypeId;
    private String leaveTypeName;
    private Integer totalDaysAllowed;
    private Integer daysUsed;
    private Integer remainingDays;
}