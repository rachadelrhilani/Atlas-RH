package com.wegrow.hr_management.service;

import com.wegrow.hr_management.dto.request.LeaveApprovalDTO;
import com.wegrow.hr_management.dto.request.LeaveRequestCreateDTO;
import com.wegrow.hr_management.dto.response.LeaveRequestResponseDTO;
import com.wegrow.hr_management.model.LeaveStatus;

import java.util.List;

public interface LeaveRequestService {
    LeaveRequestResponseDTO createRequest(LeaveRequestCreateDTO dto);
    List<LeaveRequestResponseDTO> getEmployeeRequests(Long employeeId, LeaveStatus status);
    LeaveRequestResponseDTO cancelRequest(Long requestId, Long employeeId);
    List<LeaveRequestResponseDTO> getPendingTeamRequests(Long managerId);
    LeaveRequestResponseDTO processRequestDecision(Long requestId, Long managerId, LeaveApprovalDTO approvalDTO);
}