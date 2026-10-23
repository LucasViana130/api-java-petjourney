package br.com.fiap.petjourney.dtos.response;

import java.util.List;

public record SmartCareRankingResponse(
        List<SmartCarePetAnalysisResponse> analises,
        List<SmartCareInsufficientDataPetResponse> semDadosSuficientes
) {
}
