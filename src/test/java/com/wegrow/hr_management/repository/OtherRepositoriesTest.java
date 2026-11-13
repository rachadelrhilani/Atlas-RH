package com.wegrow.hr_management.repository;

import com.wegrow.hr_management.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OtherRepositoriesTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Test
    @DisplayName("ContractRepository.findByEmployeeId - Historique des contrats")
    void findContractsByEmployeeId() {
        Employee emp = Employee.builder()
                .matricule("EMP020")
                .firstName("Omar")
                .lastName("Fassi")
                .email("omar@wegrow.com")
                .hireDate(LocalDate.now())
                .role(Role.EMPLOYE)
                .build();
        entityManager.persist(emp);

        Contract c1 = Contract.builder()
                .type(ContractType.STAGE)
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 6, 30))
                .grossSalary(new BigDecimal("4000.00"))
                .employee(emp)
                .build();
        entityManager.persist(c1);

        Contract c2 = Contract.builder()
                .type(ContractType.CDI)
                .startDate(LocalDate.of(2025, 7, 1))
                .grossSalary(new BigDecimal("12000.00"))
                .employee(emp)
                .build();
        entityManager.persist(c2);
        entityManager.flush();

        List<Contract> contracts = contractRepository.findByEmployeeId(emp.getId());
        assertThat(contracts).hasSize(2);
    }

    @Test
    @DisplayName("DepartmentRepository & LeaveTypeRepository - Recherche par nom unique")
    void findByName_DepartmentAndLeaveType() {
        Department dept = Department.builder().name("Finance").description("Comptabilité").build();
        LeaveType leaveType = LeaveType.builder().name("Maladie").daysPerYear(10).build();

        entityManager.persist(dept);
        entityManager.persist(leaveType);
        entityManager.flush();

        Optional<Department> foundDept = departmentRepository.findByName("Finance");
        Optional<LeaveType> foundType = leaveTypeRepository.findByName("Maladie");

        assertThat(foundDept).isPresent();
        assertThat(foundType).isPresent();
        assertThat(foundType.get().getDaysPerYear()).isEqualTo(10);
    }
}