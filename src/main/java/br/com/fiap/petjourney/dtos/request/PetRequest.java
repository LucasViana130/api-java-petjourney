package br.com.fiap.petjourney.dtos.request;

import br.com.fiap.petjourney.models.enums.PetSex;
import br.com.fiap.petjourney.models.enums.PetSpecies;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PetRequest(
        @NotBlank(message = "O nome do pet é obrigatório")
        String name,

        @NotNull(message = "A espécie é obrigatória")
        PetSpecies species,

        String breed,

        PetSex sex,

        LocalDate birthDate,

        @Positive(message = "O peso deve ser positivo")
        BigDecimal weight,

        Long tutorId
) {
}
