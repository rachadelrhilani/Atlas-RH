package com.wegrow.mapper;

import com.wegrow.dto.request.LeaveRequestCreateDTO;
import com.wegrow.dto.response.LeaveRequestResponseDTO;
import com.wegrow.model.LeaveRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LeaveRequestMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeFullName", expression = "java(entity.getEmployee() != null ? entity.getEmployee().getFirstName() + ' ' + entity.getEmployee().getLastName() : null)")
    @Mapping(source = "leaveType.name", target = "leaveTypeName")
    LeaveRequestResponseDTO toDto(LeaveRequest entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "workingDays", ignore = true)
    @Mapping(target = "managerComment", ignore = true)
    @Mapping(target = "decisionDate", ignore = true)
    @Mapping(target = "employee", ignore = true)
    @Mapping(target = "leaveType", ignore = true)
    LeaveRequest toEntity(LeaveRequestCreateDTO dto);
}