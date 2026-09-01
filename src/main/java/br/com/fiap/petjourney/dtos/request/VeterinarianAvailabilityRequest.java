package br.com.fiap.petjourney.dtos.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record VeterinarianAvailabilityRequest(
        Long veterinarianId,

        @NotNull(message = "O início da disponibilidade é obrigatório")
        @Future(message = "A disponibilidade deve ser futura")
        LocalDateTime startTime,

        @NotNull(message = "O fim da disponibilidade é obrigatório")
        @Future(message = "O fim da disponibilidade deve ser futuro")
        LocalDateTime endTime
) {
}
