package com.wegrow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DepartmentRequestDTO {
    @NotBlank(message = "Le nom du département est obligatoire")
    private String name;

    private String description;
}