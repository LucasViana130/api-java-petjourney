package br.com.fiap.petjourney.dtos.response;

import java.util.List;

public record SmartCarePetAnalysisResponse(
        Long petId,
        String petName,
        int score,
        String prioridade,
        List<String> motivos,
        String recomendacao
) {

    public static SmartCarePetAnalysisResponse from(String petName, SmartCareAnalysisResponse analysis) {
        return new SmartCarePetAnalysisResponse(
                analysis.petId(),
                petName,
                analysis.score(),
                analysis.prioridade(),
                analysis.motivos(),
                analysis.recomendacao()
        );
    }
}
