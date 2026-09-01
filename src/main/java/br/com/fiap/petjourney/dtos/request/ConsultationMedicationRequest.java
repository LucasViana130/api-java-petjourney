package br.com.fiap.petjourney.dtos.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record ConsultationMedicationRequest(
        @NotBlank(message = "O nome do medicamento é obrigatório")
        String name,

        String dosage,
        String frequency,
        LocalDate startDate,
        LocalDate endDate,
        String observations
) {
}
