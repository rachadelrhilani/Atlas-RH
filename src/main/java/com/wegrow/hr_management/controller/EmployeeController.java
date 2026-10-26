package com.wegrow.hr_management.controller;

import com.wegrow.hr_management.dto.request.EmployeeRequestDTO;
import com.wegrow.hr_management.dto.response.EmployeeResponseDTO;
import com.wegrow.hr_management.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "2. Employés (RH)", description = "Gestion des fiches employés et recherches")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    @Operation(summary = "Lister et rechercher des employés", description = "Permet de lister les employés avec pagination, tri et filtres optionnels par nom/prénom ou département.")
    @ApiResponse(responseCode = "200", description = "Recherche effectuée avec succès")
    public ResponseEntity<Page<EmployeeResponseDTO>> search(
            @Parameter(description = "Filtre par nom ou prénom (recherche partielle)") @RequestParam(required = false) String name,
            @Parameter(description = "Filtre par ID du département") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "Paramètres de pagination") Pageable pageable) {
        return ResponseEntity.ok(employeeService.search(name, departmentId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter la fiche d'un employé", description = "Récupère les détails complets de la fiche d'un employé.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche employé trouvée"),
            @ApiResponse(responseCode = "404", description = "Employé non trouvé")
    })
    public ResponseEntity<EmployeeResponseDTO> getById(
            @Parameter(description = "ID de l'employé") @PathVariable Long id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    @GetMapping("/manager/{managerId}/team")
    @Operation(summary = "Consulter l'équipe d'un manager", description = "Retourne la liste de tous les employés rattachés directement à un manager donné.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Membres de l'équipe récupérés"),
            @ApiResponse(responseCode = "404", description = "Manager non trouvé")
    })
    public ResponseEntity<List<EmployeeResponseDTO>> getTeamByManager(
            @Parameter(description = "ID du manager") @PathVariable Long managerId) {
        return ResponseEntity.ok(employeeService.findTeamByManager(managerId));
    }

    @PostMapping
    @Operation(summary = "Créer la fiche d'un employé", description = "Crée une nouvelle fiche employé avec vérification d'unicité sur le matricule et l'email.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employé créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "409", description = "Matricule ou adresse e-mail déjà utilisé")
    })
    public ResponseEntity<EmployeeResponseDTO> create(@Valid @RequestBody EmployeeRequestDTO dto) {
        return new ResponseEntity<>(employeeService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier la fiche d'un employé", description = "Met à jour la fiche d'un employé. Un employé ne peut pas être défini comme son propre manager.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fiche mise à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données invalides ou tentative d'auto-management"),
            @ApiResponse(responseCode = "404", description = "Employé, département ou manager introuvable"),
            @ApiResponse(responseCode = "409", description = "Matricule ou adresse e-mail déjà utilisé par un autre employé")
    })
    public ResponseEntity<EmployeeResponseDTO> update(
            @Parameter(description = "ID de l'employé à modifier") @PathVariable Long id,
            @Valid @RequestBody EmployeeRequestDTO dto) {
        return ResponseEntity.ok(employeeService.update(id, dto));
    }
}