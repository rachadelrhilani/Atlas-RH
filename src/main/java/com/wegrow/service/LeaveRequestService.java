package com.wegrow.service;

import com.wegrow.dto.request.LeaveApprovalDTO;
import com.wegrow.dto.request.LeaveRequestCreateDTO;
import com.wegrow.dto.response.LeaveRequestResponseDTO;
import com.wegrow.model.LeaveStatus;

import java.util.List;

public interface LeaveRequestService {
    LeaveRequestResponseDTO createRequest(LeaveRequestCreateDTO dto);
    List<LeaveRequestResponseDTO> getEmployeeRequests(Long employeeId, LeaveStatus status);
    LeaveRequestResponseDTO cancelRequest(Long requestId, Long employeeId);
    List<LeaveRequestResponseDTO> getPendingTeamRequests(Long managerId);
    LeaveRequestResponseDTO processRequestDecision(Long requestId, Long managerId, LeaveApprovalDTO approvalDTO);
}