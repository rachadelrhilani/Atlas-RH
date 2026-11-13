package com.wegrow.hr_management.repository;

import com.wegrow.hr_management.model.Department;
import com.wegrow.hr_management.model.Employee;
import com.wegrow.hr_management.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class EmployeeRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EmployeeRepository employeeRepository;

    private Department itDept;
    private Department hrDept;
    private Employee manager;

    @BeforeEach
    void setUp() {
        itDept = Department.builder()
                .name("Ingénierie IT")
                .description("Développement")
                .build();
        entityManager.persist(itDept);

        hrDept = Department.builder()
                .name("Ressources Humaines")
                .description("RH")
                .build();
        entityManager.persist(hrDept);

        manager = Employee.builder()
                .matricule("MGR001")
                .firstName("Karim")
                .lastName("Alami")
                .email("karim@wegrow.com")
                .hireDate(LocalDate.of(2022, 1, 10))
                .role(Role.MANAGER)
                .department(itDept)
                .build();
        entityManager.persist(manager);

        Employee emp1 = Employee.builder()
                .matricule("EMP001")
                .firstName("Rachad")
                .lastName("El Rhilani")
                .email("rachad@wegrow.com")
                .hireDate(LocalDate.of(2024, 3, 1))
                .role(Role.EMPLOYE)
                .department(itDept)
                .manager(manager)
                .build();
        entityManager.persist(emp1);

        Employee emp2 = Employee.builder()
                .matricule("EMP002")
                .firstName("Sara")
                .lastName("Bennani")
                .email("sara@wegrow.com")
                .hireDate(LocalDate.of(2023, 6, 15))
                .role(Role.EMPLOYE)
                .department(hrDept)
                .manager(manager)
                .build();
        entityManager.persist(emp2);

        Employee emp3 = Employee.builder()
                .matricule("EMP003")
                .firstName("Youssef")
                .lastName("El Idrissi")
                .email("youssef@wegrow.com")
                .hireDate(LocalDate.of(2025, 2, 1))
                .role(Role.EMPLOYE)
                .department(itDept)
                .build();
        entityManager.persist(emp3);

        entityManager.flush();
    }

    @Test
    @DisplayName("findByMatricule et findByEmail - Recherche exacte")
    void findByMatriculeAndEmail_ReturnsEmployee() {
        Optional<Employee> byMatricule = employeeRepository.findByMatricule("EMP001");
        Optional<Employee> byEmail = employeeRepository.findByEmail("sara@wegrow.com");

        assertThat(byMatricule).isPresent();
        assertThat(byMatricule.get().getFirstName()).isEqualTo("Rachad");

        assertThat(byEmail).isPresent();
        assertThat(byEmail.get().getMatricule()).isEqualTo("EMP002");
    }

    @Test
    @DisplayName("findByManagerId - Récupère les membres de l'équipe d'un manager")
    void findByManagerId_ReturnsTeamMembers() {
        List<Employee> team = employeeRepository.findByManagerId(manager.getId());

        assertThat(team).hasSize(2);
        assertThat(team).extracting(Employee::getMatricule)
                .containsExactlyInAnyOrder("EMP001", "EMP002");
    }

    @Test
    @DisplayName("findByDepartmentId - Filtre par département avec pagination et tri")
    void findByDepartmentId_WithPaginationAndSort() {
        Pageable pageable = PageRequest.of(0, 2, Sort.by("firstName").ascending());
        Page<Employee> page = employeeRepository.findByDepartmentId(itDept.getId(), pageable);

        assertThat(page.getTotalElements()).isEqualTo(3); // Karim, Rachad, Youssef
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).getFirstName()).isEqualTo("Karim");
        assertThat(page.getContent().get(1).getFirstName()).isEqualTo("Rachad");
    }

    @Test
    @DisplayName("searchByName (@Query) - Recherche partielle insensible à la casse sur prénom ou nom")
    void searchByName_CaseInsensitivePartialMatch() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("lastName").descending());
        Page<Employee> results = employeeRepository.searchByName("el", pageable);

        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent().get(0).getLastName()).isEqualTo("El Rhilani");
        assertThat(results.getContent().get(1).getLastName()).isEqualTo("El Idrissi");
    }

    @Test
    @DisplayName("searchByDepartmentAndName (@Query) - Recherche combinée département + nom")
    void searchByDepartmentAndName_FiltersBothCriteria() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Employee> results = employeeRepository.searchByDepartmentAndName(itDept.getId(), "a", pageable);

        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent()).extracting(Employee::getFirstName)
                .containsExactlyInAnyOrder("Karim", "Rachad");
    }
}