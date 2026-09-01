package br.com.fiap.petjourney.dtos.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ScheduleAppointmentRequest(
        @NotBlank(message = "O título é obrigatório")
        String title,

        String description,

        @NotNull(message = "O ID do pet é obrigatório")
        Long petId,

        Long clinicId,

        @NotNull(message = "O ID do veterinário é obrigatório")
        Long veterinarianId,

        @NotNull(message = "A data e hora são obrigatórias")
        @Future(message = "O agendamento deve ser para uma data futura")
        LocalDateTime dateTime
) {
}
