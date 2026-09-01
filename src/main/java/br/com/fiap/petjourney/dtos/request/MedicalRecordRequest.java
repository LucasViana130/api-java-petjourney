package br.com.fiap.petjourney.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MedicalRecordRequest(
    @NotNull(message = "A data de registro é obrigatória")
    LocalDateTime registrationDate,

    @NotBlank(message = "A queixa principal é obrigatória")
    String mainComplaint,

    String diagnosis,
    String conduct,
    String observations,
    String clinicalNotes,
    String recommendations,
    String prescriptionNotes,

    Long appointmentId,

    @NotNull(message = "O ID do pet é obrigatório")
    Long petId,

    @NotNull(message = "O ID do veterinário é obrigatório")
    Long veterinarianId
) {}
