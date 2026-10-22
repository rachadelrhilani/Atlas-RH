package com.wegrow.dto.response;

import com.wegrow.model.Role;
import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeResponseDTO {
    private Long id;
    private String matricule;
    private String firstName;
    private String lastName;
    private String email;
    private String position;
    private LocalDate hireDate;
    private Role role;

    private Long departmentId;
    private String departmentName;
    private Long managerId;
    private String managerFullName;
}