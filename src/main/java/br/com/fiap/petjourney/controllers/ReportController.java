package br.com.fiap.petjourney.controllers;

import br.com.fiap.petjourney.dtos.response.PetReportItemResponse;
import br.com.fiap.petjourney.dtos.response.PetWeightReportItemResponse;
import br.com.fiap.petjourney.models.enums.PetSpecies;
import br.com.fiap.petjourney.services.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Relatorios clinicos gerados por procedures PostgreSQL")
public class ReportController {

    private final ReportService service;

    @GetMapping("/pets")
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    @Operation(summary = "Listar pets da clinica autenticada")
    public ResponseEntity<List<PetReportItemResponse>> pets() {
        return ResponseEntity.ok(service.pets());
    }

    @GetMapping("/pets/weight")
    @PreAuthorize("hasRole('ADMIN_CLINICA')")
    @Operation(summary = "Consolidar peso dos pets da clinica autenticada")
    public ResponseEntity<List<PetWeightReportItemResponse>> petWeights(
            @RequestParam(required = false) PetSpecies species
    ) {
        return ResponseEntity.ok(service.petWeights(species));
    }
}
