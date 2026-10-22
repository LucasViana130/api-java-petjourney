package br.com.fiap.petjourney.dtos.response;

public record PetReportItemResponse(
        Long petId,
        Long tutorId,
        Long clinicId,
        String pet,
        String tutor,
        String clinica
) {
}
