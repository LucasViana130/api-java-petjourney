package br.com.fiap.petjourney.dtos.response;

import br.com.fiap.petjourney.models.enums.PetSex;
import br.com.fiap.petjourney.models.enums.PetSpecies;

import java.math.BigDecimal;

public record PetWeightReportItemResponse(
        Type tipo,
        PetSpecies species,
        PetSex sex,
        BigDecimal pesoKg
) {
    public enum Type {
        DETALHE,
        SUBTOTAL,
        TOTAL
    }
}
