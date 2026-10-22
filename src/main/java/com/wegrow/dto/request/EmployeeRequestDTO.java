package com.wegrow.dto.request;

import com.wegrow.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeRequestDTO {
    @NotBlank(message = "Le matricule est obligatoire")
    private String matricule;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @Email(message = "Format d'email invalide")
    @NotBlank(message = "L'email est obligatoire")
    private String email;

    private String position;
    
    @NotNull(message = "La date d'embauche est obligatoire")
    private LocalDate hireDate;

    @NotNull(message = "Le rôle est obligatoire")
    private Role role;

    private Long departmentId;
    private Long managerId;
}