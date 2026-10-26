package com.wegrow.hr_management.controller;

import com.wegrow.hr_management.dto.request.ContractRequestDTO;
import com.wegrow.hr_management.dto.response.ContractResponseDTO;
import com.wegrow.hr_management.service.ContractService;
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
@RequestMapping("/api/v1/contracts")
@Tag(name = "3. Contrats (RH)", description = "Gestion des contrats de travail (US3)")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @PostMapping
    @Operation(summary = "Créer un contrat", description = "Crée un contrat pour un employé (CDI, CDD, STAGE). Vérifie qu'aucun autre contrat n'est en cours et exige une date de fin pour CDD et STAGE.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contrat créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Un contrat actif existe déjà ou la date de fin est manquante pour un CDD/STAGE"),
            @ApiResponse(responseCode = "404", description = "Employé non trouvé")
    })
    public ResponseEntity<ContractResponseDTO> createContract(@Valid @RequestBody ContractRequestDTO dto) {
        return new ResponseEntity<>(contractService.createContract(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/terminate")
    @Operation(summary = "Terminer un contrat en cours", description = "Clôture un contrat actif en fixant sa date de fin à la date du jour.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contrat clôturé avec succès"),
            @ApiResponse(responseCode = "400", description = "Le contrat est déjà arrivé à terme ou déjà clôturé"),
            @ApiResponse(responseCode = "404", description = "Contrat non trouvé")
    })
    public ResponseEntity<ContractResponseDTO> terminateContract(
            @Parameter(description = "ID du contrat à clôturer") @PathVariable Long id) {
        return ResponseEntity.ok(contractService.terminateContract(id));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Consulter l'historique des contrats", description = "Retourne l'historique complet de tous les contrats créés pour un employé.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historique des contrats récupéré"),
            @ApiResponse(responseCode = "404", description = "Employé non trouvé")
    })
    public ResponseEntity<List<ContractResponseDTO>> getEmployeeContractHistory(
            @Parameter(description = "ID de l'employé") @PathVariable Long employeeId) {
        return ResponseEntity.ok(contractService.getEmployeeContractHistory(employeeId));
    }
}