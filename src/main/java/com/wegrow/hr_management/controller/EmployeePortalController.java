package com.wegrow.hr_management.controller;

import com.wegrow.hr_management.dto.request.LeaveRequestCreateDTO;
import com.wegrow.hr_management.dto.response.LeaveBalanceResponseDTO;
import com.wegrow.hr_management.dto.response.LeaveRequestResponseDTO;
import com.wegrow.hr_management.dto.response.SeniorityResponseDTO;
import com.wegrow.hr_management.model.LeaveStatus;
import com.wegrow.hr_management.service.EmployeeService;
import com.wegrow.hr_management.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employee-portal")
@Tag(name = "5. Espace Employé", description = "Portail employé : demandes de congés, suivi des soldes et ancienneté")
public class EmployeePortalController {

    private final LeaveRequestService leaveRequestService;
    private final EmployeeService employeeService;

    public EmployeePortalController(LeaveRequestService leaveRequestService, EmployeeService employeeService) {
        this.leaveRequestService = leaveRequestService;
        this.employeeService = employeeService;
    }

    @PostMapping("/leave-requests")
    @Operation(summary = "Soumettre une demande de congé (US5)", description = "Permet à l'employé de créer une demande de congé avec motif et dates.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Demande créée avec succès"),
            @ApiResponse(responseCode = "400", description = "Dates invalides, solde insuffisant ou chevauchement"),
            @ApiResponse(responseCode = "404", description = "Employé ou type de congé introuvable")
    })
    public ResponseEntity<LeaveRequestResponseDTO> createRequest(@Valid @RequestBody LeaveRequestCreateDTO dto) {
        return new ResponseEntity<>(leaveRequestService.createRequest(dto), HttpStatus.CREATED);
    }

    @GetMapping("/{employeeId}/leave-requests")
    @Operation(summary = "Consulter ses demandes de congé", description = "Retourne l'historique de ses demandes, filtrables par statut (EN_ATTENTE, APPROUVEE, REFUSEE, ANNULEE).")
    @ApiResponse(responseCode = "200", description = "Historique récupéré avec succès")
    public ResponseEntity<List<LeaveRequestResponseDTO>> getEmployeeRequests(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeeId,
            @Parameter(description = "Filtre optionnel par statut") @RequestParam(required = false) LeaveStatus status) {
        return ResponseEntity.ok(leaveRequestService.getEmployeeRequests(employeeId, status));
    }

    @PutMapping("/{employeeId}/leave-requests/{requestId}/cancel")
    @Operation(summary = "Annuler une demande de congé", description = "Permet d'annuler sa propre demande tant qu'elle est au statut EN_ATTENTE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Demande annulée avec succès"),
            @ApiResponse(responseCode = "400", description = "Impossible d'annuler une demande déjà traitée"),
            @ApiResponse(responseCode = "404", description = "Demande introuvable")
    })
    public ResponseEntity<LeaveRequestResponseDTO> cancelRequest(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeeId,
            @Parameter(description = "ID de la demande") @PathVariable Long requestId) {
        return ResponseEntity.ok(leaveRequestService.cancelRequest(requestId, employeeId));
    }

    @GetMapping("/{employeeId}/balances")
    @Operation(summary = "Consulter son solde de congés", description = "Calcule et retourne le total alloué, les jours consommés de l'année en cours et le reste par type de congé.")
    @ApiResponse(responseCode = "200", description = "Soldes calculés et retournés avec succès")
    public ResponseEntity<List<LeaveBalanceResponseDTO>> getLeaveBalances(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getLeaveBalances(employeeId));
    }

    @GetMapping("/{employeeId}/seniority")
    @Operation(summary = "Consulter son ancienneté", description = "Calcule l'ancienneté précise en années et mois depuis la date d'embauche.")
    @ApiResponse(responseCode = "200", description = "Ancienneté calculée avec succès")
    public ResponseEntity<SeniorityResponseDTO> getSeniority(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getSeniority(employeeId));
    }
}