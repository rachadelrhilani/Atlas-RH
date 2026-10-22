package com.wegrow.mapper;

import com.wegrow.dto.request.ContractRequestDTO;
import com.wegrow.dto.response.ContractResponseDTO;
import com.wegrow.model.Contract;
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