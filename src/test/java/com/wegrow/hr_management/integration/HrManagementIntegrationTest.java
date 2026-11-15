package com.wegrow.hr_management.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wegrow.hr_management.dto.request.DepartmentRequestDTO;
import com.wegrow.hr_management.dto.request.EmployeeRequestDTO;
import com.wegrow.hr_management.dto.request.LeaveApprovalDTO;
import com.wegrow.hr_management.dto.request.LeaveRequestCreateDTO;
import com.wegrow.hr_management.model.LeaveStatus;
import com.wegrow.hr_management.model.LeaveType;
import com.wegrow.hr_management.model.Role;
import com.wegrow.hr_management.repository.LeaveTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.generate-ddl=true",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false"
})
@Transactional
class HrManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    private LeaveType paidLeaveType;

    @BeforeEach
    void setUpDatabase() {
        // Initialisation du référentiel de type de congé dans la base dédiée aux tests
        paidLeaveType = LeaveType.builder()
                .name("Congé Payé Intégration")
                .daysPerYear(22)
                .build();
        paidLeaveType = leaveTypeRepository.save(paidLeaveType);
    }

    @Test
    @DisplayName("Scénario E2E 1 : Créer Département -> Manager -> Employé -> Demande de congé -> Approbation -> Vérification du solde")
    void endToEnd_LeaveRequestCreationAndApprovalWorkflow() throws Exception {
        // 1. Créer un Département via POST /api/v1/departments
        DepartmentRequestDTO deptReq = new DepartmentRequestDTO();
        deptReq.setName("Développement Backend");
        deptReq.setDescription("Équipe Java Spring Boot");

        MvcResult deptResult = mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long departmentId = objectMapper.readTree(deptResult.getResponse().getContentAsString()).get("id").asLong();

        // 2. Créer un Manager via POST /api/v1/employees
        EmployeeRequestDTO managerReq = new EmployeeRequestDTO();
        managerReq.setMatricule("MGR-INT-01");
        managerReq.setFirstName("Karim");
        managerReq.setLastName("Alami");
        managerReq.setEmail("karim.int@wegrow.com");
        managerReq.setPosition("Tech Lead");
        managerReq.setHireDate(LocalDate.of(2022, 1, 10));
        managerReq.setRole(Role.MANAGER);
        managerReq.setDepartmentId(departmentId);

        MvcResult managerResult = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(managerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long managerId = objectMapper.readTree(managerResult.getResponse().getContentAsString()).get("id").asLong();

        // 3. Créer un Employé rattaché à ce Manager et à ce Département
        EmployeeRequestDTO empReq = new EmployeeRequestDTO();
        empReq.setMatricule("EMP-INT-02");
        empReq.setFirstName("Rachad");
        empReq.setLastName("El Rhilani");
        empReq.setEmail("rachad.int@wegrow.com");
        empReq.setPosition("Développeur Full-Stack");
        empReq.setHireDate(LocalDate.of(2024, 6, 1));
        empReq.setRole(Role.EMPLOYE);
        empReq.setDepartmentId(departmentId);
        empReq.setManagerId(managerId);

        MvcResult empResult = mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long employeeId = objectMapper.readTree(empResult.getResponse().getContentAsString()).get("id").asLong();

        // 4. L'employé soumet une demande de congé de 5 jours ouvrés (prochain Lundi au Vendredi)
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextFriday = nextMonday.plusDays(4);

        LeaveRequestCreateDTO leaveReq = new LeaveRequestCreateDTO();
        leaveReq.setEmployeeId(employeeId);
        leaveReq.setLeaveTypeId(paidLeaveType.getId());
        leaveReq.setStartDate(nextMonday);
        leaveReq.setEndDate(nextFriday);
        leaveReq.setReason("Congés annuels");

        MvcResult leaveResult = mockMvc.perform(post("/api/v1/employee-portal/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leaveReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.workingDays").value(5))
                .andReturn();

        Long requestId = objectMapper.readTree(leaveResult.getResponse().getContentAsString()).get("id").asLong();

        // 5. Le Manager consulte les demandes en attente de son équipe
        mockMvc.perform(get("/api/v1/manager/" + managerId + "/pending-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(requestId));

        // 6. Le Manager approuve la demande
        LeaveApprovalDTO approvalDTO = new LeaveApprovalDTO();
        approvalDTO.setStatus(LeaveStatus.APPROUVEE);
        approvalDTO.setManagerComment("Approuvé pour l'équipe.");

        mockMvc.perform(put("/api/v1/manager/" + managerId + "/leave-requests/" + requestId + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approvalDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROUVEE"))
                .andExpect(jsonPath("$.managerComment").value("Approuvé pour l'équipe."));

        // 7. Vérifier en base via l'API que le solde de l'employé a bien été déduit (22 - 5 = 17 jours restants)
        mockMvc.perform(get("/api/v1/employee-portal/" + employeeId + "/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].leaveTypeName").value("Congé Payé Intégration"))
                .andExpect(jsonPath("$[0].daysUsed").value(5))
                .andExpect(jsonPath("$[0].remainingDays").value(17));
    }

    @Test
    @DisplayName("Scénario E2E 2 : Refuser la suppression d'un département qui contient encore des employés")
    void endToEnd_DeleteDepartmentWithEmployees_IsRejected() throws Exception {
        // 1. Créer un Département
        DepartmentRequestDTO deptReq = new DepartmentRequestDTO();
        deptReq.setName("Département Finance");
        deptReq.setDescription("Comptabilité et Audit");

        MvcResult deptResult = mockMvc.perform(post("/api/v1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode deptJson = objectMapper.readTree(deptResult.getResponse().getContentAsString());
        Long departmentId = deptJson.get("id").asLong();

        // 2. Rattacher un Employé à ce Département
        EmployeeRequestDTO empReq = new EmployeeRequestDTO();
        empReq.setMatricule("EMP-FIN-01");
        empReq.setFirstName("Salma");
        empReq.setLastName("Bennani");
        empReq.setEmail("salma.fin@wegrow.com");
        empReq.setPosition("Contrôleuse de gestion");
        empReq.setHireDate(LocalDate.of(2023, 9, 1));
        empReq.setRole(Role.EMPLOYE);
        empReq.setDepartmentId(departmentId);

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(empReq)))
                .andExpect(status().isCreated());

      
        mockMvc.perform(delete("/api/v1/departments/" + departmentId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(get("/api/v1/departments/" + departmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Département Finance"));
    }
}