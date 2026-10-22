package com.wegrow.dto.response;

import lombok.Data;

@Data
public class DepartmentResponseDTO {
    private Long id;
    private String name;
    private String description;
    private Long employeeCount; 
}