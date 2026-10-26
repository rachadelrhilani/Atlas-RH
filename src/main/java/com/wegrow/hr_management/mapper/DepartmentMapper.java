package com.wegrow.hr_management.mapper;

import com.wegrow.hr_management.dto.request.DepartmentRequestDTO;
import com.wegrow.hr_management.dto.response.DepartmentResponseDTO;
import com.wegrow.hr_management.model.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {

    @Mapping(target = "employeeCount", expression = "java(entity.getEmployees() != null ? (long) entity.getEmployees().size() : 0L)")
    DepartmentResponseDTO toDto(Department entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    Department toEntity(DepartmentRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employees", ignore = true)
    void updateEntityFromDto(DepartmentRequestDTO dto, @MappingTarget Department entity);
}