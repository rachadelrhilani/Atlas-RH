package com.wegrow.hr_management.repository;

import com.wegrow.hr_management.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class LeaveRequestRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private Employee emp1;
    private Employee emp2;

    @BeforeEach
    void setUp() {
        LeaveType paidLeave = LeaveType.builder()
                .name("Congé Payé")
                .daysPerYear(22)
                .build();
        entityManager.persist(paidLeave);

        emp1 = Employee.builder()
                .matricule("EMP010")
                .firstName("Youssef")
                .lastName("Bennani")
                .email("youssef.b@wegrow.com")
                .hireDate(LocalDate.of(2024, 1, 1))
                .role(Role.EMPLOYE)
                .build();
        entityManager.persist(emp1);

        emp2 = Employee.builder()
                .matricule("EMP011")
                .firstName("Salma")
                .lastName("Tazi")
                .email("salma.t@wegrow.com")
                .hireDate(LocalDate.of(2024, 5, 1))
                .role(Role.EMPLOYE)
                .build();
        entityManager.persist(emp2);

        LeaveRequest req1 = LeaveRequest.builder()
                .employee(emp1)
                .leaveType(paidLeave)
                .startDate(LocalDate.of(2026, 11, 2))
                .endDate(LocalDate.of(2026, 11, 6))
                .workingDays(5)
                .status(LeaveStatus.EN_ATTENTE)
                .build();
        entityManager.persist(req1);

        LeaveRequest req2 = LeaveRequest.builder()
                .employee(emp1)
                .leaveType(paidLeave)
                .startDate(LocalDate.of(2026, 8, 3))
                .endDate(LocalDate.of(2026, 8, 7))
                .workingDays(5)
                .status(LeaveStatus.APPROUVEE)
                .build();
        entityManager.persist(req2);

        LeaveRequest req3 = LeaveRequest.builder()
                .employee(emp2)
                .leaveType(paidLeave)
                .startDate(LocalDate.of(2026, 12, 1))
                .endDate(LocalDate.of(2026, 12, 4))
                .workingDays(4)
                .status(LeaveStatus.EN_ATTENTE)
                .build();
        entityManager.persist(req3);

        entityManager.flush();
    }

    @Test
    @DisplayName("findByEmployeeId - Retourne toutes les demandes d'un employé")
    void findByEmployeeId_ReturnsAllEmployeeRequests() {
        List<LeaveRequest> requests = leaveRequestRepository.findByEmployeeId(emp1.getId());
        assertThat(requests).hasSize(2);
    }

    @Test
    @DisplayName("findByEmployeeIdAndStatus - Filtre les demandes par statut")
    void findByEmployeeIdAndStatus_FiltersByStatus() {
        List<LeaveRequest> approved = leaveRequestRepository.findByEmployeeIdAndStatus(emp1.getId(), LeaveStatus.APPROUVEE);

        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).getWorkingDays()).isEqualTo(5);
    }

    @Test
    @DisplayName("findByEmployeeIdInAndStatus - Récupère les demandes en attente de toute une équipe")
    void findByEmployeeIdInAndStatus_ReturnsTeamPendingRequests() {
        List<LeaveRequest> teamPending = leaveRequestRepository.findByEmployeeIdInAndStatus(
                List.of(emp1.getId(), emp2.getId()),
                LeaveStatus.EN_ATTENTE
        );

        assertThat(teamPending).hasSize(2);
        assertThat(teamPending).extracting(r -> r.getEmployee().getMatricule())
                .containsExactlyInAnyOrder("EMP010", "EMP011");
    }
}