package com.wegrow.hr_management.mapper;

import com.wegrow.hr_management.dto.request.ContractRequestDTO;
import com.wegrow.hr_management.dto.response.ContractResponseDTO;
import com.wegrow.hr_management.model.Contract;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractMapper {

    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(target = "employeeFullName", expression = "java(entity.getEmployee() != null ? entity.getEmployee().getFirstName() + ' ' + entity.getEmployee().getLastName() : null)")
    ContractResponseDTO toDto(Contract entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employee", ignore = true)
    Contract toEntity(ContractRequestDTO dto);
}