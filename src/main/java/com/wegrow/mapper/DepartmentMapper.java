package com.wegrow.mapper;

import com.wegrow.dto.request.DepartmentRequestDTO;
import com.wegrow.dto.response.DepartmentResponseDTO;
import com.wegrow.model.Department;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DepartmentMapper {
    DepartmentResponseDTO toDto(Department entity);
    Department toEntity(DepartmentRequestDTO dto);
    void updateEntityFromDto(DepartmentRequestDTO dto, @MappingTarget Department entity);
}