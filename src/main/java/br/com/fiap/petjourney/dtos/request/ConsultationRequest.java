package br.com.fiap.petjourney.dtos.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ConsultationRequest(
        @NotNull(message = "O ID do agendamento é obrigatório")
        Long appointmentId,

        @NotBlank(message = "A queixa principal é obrigatória")
        String mainComplaint,

        String diagnosis,
        String conduct,
        String observations,
        String clinicalNotes,
        String recommendations,
        String prescriptionNotes,

        @Valid
        List<ConsultationMedicationRequest> medications
) {
}
