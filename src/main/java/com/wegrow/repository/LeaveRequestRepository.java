package com.wegrow.repository;

import com.wegrow.model.LeaveRequest;
import com.wegrow.model.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, LeaveStatus status);
    List<LeaveRequest> findByEmployeeIdInAndStatus(List<Long> employeeIds, LeaveStatus status);
}