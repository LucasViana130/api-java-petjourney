package br.com.fiap.petjourney.dtos.request;

import java.time.LocalDate;

public record SmartCareAnalysisRequest(
        Long petId,
        LocalDate dataUltimoAtendimento,
        boolean possuiProximoAgendamento,
        boolean possuiTratamentoRegistrado
) {
}
