package com.wegrow.hr_management.controller;

import com.wegrow.hr_management.dto.request.DepartmentRequestDTO;
import com.wegrow.hr_management.dto.response.DepartmentResponseDTO;
import com.wegrow.hr_management.service.DepartmentService;
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

@RestController
@RequestMapping("/api/v1/departments")
@Tag(name = "1. Départements (RH)", description = "Gestion des départements de l'entreprise (US1)")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping
    @Operation(summary = "Lister les départements", description = "Récupère la liste de tous les départements avec support de la pagination et du tri.")
    @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
    public ResponseEntity<Page<DepartmentResponseDTO>> getAll(
            @Parameter(description = "Paramètres de pagination (page, size, sort)") Pageable pageable) {
        return ResponseEntity.ok(departmentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un département", description = "Récupère les détails d'un département par son identifiant unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Département trouvé"),
            @ApiResponse(responseCode = "404", description = "Département non trouvé")
    })
    public ResponseEntity<DepartmentResponseDTO> getById(
            @Parameter(description = "ID du département") @PathVariable Long id) {
        return ResponseEntity.ok(departmentService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un département", description = "Ajoute un nouveau département. Le nom doit être unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Département créé avec succès"),
            @ApiResponse(responseCode = "400", description = "Données du formulaire invalides"),
            @ApiResponse(responseCode = "409", description = "Nom de département déjà existant")
    })
    public ResponseEntity<DepartmentResponseDTO> create(@Valid @RequestBody DepartmentRequestDTO dto) {
        return new ResponseEntity<>(departmentService.create(dto), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un département", description = "Met à jour les informations d'un département existant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Département mis à jour avec succès"),
            @ApiResponse(responseCode = "400", description = "Données du formulaire invalides"),
            @ApiResponse(responseCode = "404", description = "Département non trouvé"),
            @ApiResponse(responseCode = "409", description = "Nom de département déjà utilisé par un autre enregistrement")
    })
    public ResponseEntity<DepartmentResponseDTO> update(
            @Parameter(description = "ID du département à modifier") @PathVariable Long id,
            @Valid @RequestBody DepartmentRequestDTO dto) {
        return ResponseEntity.ok(departmentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un département", description = "Supprime un département. Échoue si le département contient encore des employés.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Département supprimé avec succès"),
            @ApiResponse(responseCode = "400", description = "Le département contient des employés (suppression interdite)"),
            @ApiResponse(responseCode = "404", description = "Département non trouvé")
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID du département à supprimer") @PathVariable Long id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}