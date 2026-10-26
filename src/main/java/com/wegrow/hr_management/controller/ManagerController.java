package com.wegrow.hr_management.controller;

import com.wegrow.hr_management.dto.request.LeaveApprovalDTO;
import com.wegrow.hr_management.dto.response.LeaveRequestResponseDTO;
import com.wegrow.hr_management.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/manager/{managerId}")
@Tag(name = "4. Espace Manager (US4)", description = "Gestion et approbation des congés de l'équipe par le Manager")
public class ManagerController {

    private final LeaveRequestService leaveRequestService;

    public ManagerController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @GetMapping("/pending-requests")
    @Operation(summary = "Lister les demandes en attente de l'équipe", description = "Récupère toutes les demandes de congé au statut EN_ATTENTE pour les membres rattachés au manager.")
    @ApiResponse(responseCode = "200", description = "Liste des demandes récupérée avec succès")
    public ResponseEntity<List<LeaveRequestResponseDTO>> getPendingTeamRequests(
            @Parameter(description = "ID du manager") @PathVariable Long managerId) {
        return ResponseEntity.ok(leaveRequestService.getPendingTeamRequests(managerId));
    }

    @PutMapping("/leave-requests/{requestId}/decision")
    @Operation(summary = "Approuver ou Refuser une demande de congé", description = "Permet d'enregistrer la décision du manager (APPROUVEE ou REFUSEE) avec un commentaire.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Décision enregistrée avec succès"),
            @ApiResponse(responseCode = "400", description = "Statut invalide ou demande déjà traitée"),
            @ApiResponse(responseCode = "404", description = "Demande ou Manager introuvable")
    })
    public ResponseEntity<LeaveRequestResponseDTO> processDecision(
            @Parameter(description = "ID du manager") @PathVariable Long managerId,
            @Parameter(description = "ID de la demande de congé") @PathVariable Long requestId,
            @Valid @RequestBody LeaveApprovalDTO approvalDTO) {
        return ResponseEntity.ok(leaveRequestService.processRequestDecision(requestId, managerId, approvalDTO));
    }
}