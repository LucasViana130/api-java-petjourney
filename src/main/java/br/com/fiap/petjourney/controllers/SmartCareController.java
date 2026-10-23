package br.com.fiap.petjourney.controllers;

import br.com.fiap.petjourney.dtos.response.SmartCareAnalysisResponse;
import br.com.fiap.petjourney.dtos.response.SmartCareRankingResponse;
import br.com.fiap.petjourney.services.SmartCareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/smartcare")
@RequiredArgsConstructor
@Tag(name = "SmartCare", description = "Analise assistencial derivada dos dados clinicos do pet")
public class SmartCareController {

    private final SmartCareService service;

    @GetMapping("/pets/{petId}/analysis")
    @PreAuthorize("hasAnyRole('ADMIN_CLINICA', 'VETERINARIO')")
    @Operation(summary = "Analisar a prioridade assistencial de um pet")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analise retornada"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao"),
            @ApiResponse(responseCode = "422", description = "Pet sem dados clinicos suficientes"),
            @ApiResponse(responseCode = "503", description = "Servico SmartCare indisponivel")
    })
    public ResponseEntity<SmartCareAnalysisResponse> analyze(@PathVariable Long petId) {
        return ResponseEntity.ok(service.analyze(petId));
    }

    @GetMapping("/ranking")
    @PreAuthorize("hasAnyRole('ADMIN_CLINICA', 'VETERINARIO')")
    @Operation(summary = "Listar pets por prioridade de acompanhamento SmartCare")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ranking retornado"),
            @ApiResponse(responseCode = "403", description = "Perfil sem permissao"),
            @ApiResponse(responseCode = "503", description = "Servico SmartCare indisponivel")
    })
    public ResponseEntity<SmartCareRankingResponse> ranking() {
        return ResponseEntity.ok(service.ranking());
    }
}
