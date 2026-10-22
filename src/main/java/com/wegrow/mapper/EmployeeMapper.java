package com.wegrow.mapper;

import com.wegrow.dto.request.EmployeeRequestDTO;
import com.wegrow.dto.response.EmployeeResponseDTO;
import com.wegrow.model.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {

    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "manager.id", target = "managerId")
    @Mapping(target = "managerFullName", expression = "java(entity.getManager() != null ? entity.getManager().getFirstName() + ' ' + entity.getManager().getLastName() : null)")
    EmployeeResponseDTO toDto(Employee entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "equipe", ignore = true)
    @Mapping(target = "contrats", ignore = true)
    @Mapping(target = "demandes", ignore = true)
    Employee toEntity(EmployeeRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "department", ignore = true)
    @Mapping(target = "manager", ignore = true)
    @Mapping(target = "equipe", ignore = true)
    @Mapping(target = "contrats", ignore = true)
    @Mapping(target = "demandes", ignore = true)
    void updateEntityFromDto(EmployeeRequestDTO dto, @MappingTarget Employee entity);
}