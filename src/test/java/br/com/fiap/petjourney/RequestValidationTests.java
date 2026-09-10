package br.com.fiap.petjourney;

import br.com.fiap.petjourney.dtos.request.AppointmentRequest;
import br.com.fiap.petjourney.dtos.request.FirstAccessRequest;
import br.com.fiap.petjourney.dtos.request.PetRequest;
import br.com.fiap.petjourney.dtos.request.TutorRequest;
import br.com.fiap.petjourney.models.enums.AppointmentStatus;
import br.com.fiap.petjourney.models.enums.PetSpecies;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTests {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void tutorRequestRejectsInvalidCpfAndOversizedEmail() {
        var request = new TutorRequest(
                "Tutor Teste",
                "123",
                "11999999999",
                "a".repeat(250) + "@email.com"
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("cpf", "email");
    }

    @Test
    void petRequestRejectsFutureBirthDateAndNegativeTutorId() {
        var request = new PetRequest(
                "Bolt",
                PetSpecies.CACHORRO,
                "SRD",
                null,
                LocalDate.now().plusDays(1),
                new BigDecimal("12.5"),
                -1L
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("birthDate", "tutorId");
    }

    @Test
    void appointmentRequestRejectsPastDateAndInvalidIds() {
        var request = new AppointmentRequest(
                "Consulta",
                "Retorno",
                LocalDateTime.now().minusDays(1),
                AppointmentStatus.PENDENTE,
                0L,
                -1L,
                -2L
        );

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("dateTime", "petId", "veterinarianId", "clinicId");
    }

    @Test
    void firstAccessRequestRejectsInvalidEmailCodeAndPassword() {
        var request = new FirstAccessRequest("email-invalido", "123", "12345");

        var violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("username", "code", "password");
    }
}
