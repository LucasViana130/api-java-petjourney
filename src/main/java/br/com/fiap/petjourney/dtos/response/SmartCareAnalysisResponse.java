package br.com.fiap.petjourney.dtos.response;

import java.util.List;

public record SmartCareAnalysisResponse(
        Long petId,
        Integer score,
        String prioridade,
        List<String> motivos,
        String recomendacao
) {
}
