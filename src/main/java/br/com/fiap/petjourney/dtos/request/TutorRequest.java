package br.com.fiap.petjourney.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TutorRequest(
        @NotBlank(message = "O nome e obrigatorio")
        String name,

        @NotBlank(message = "O CPF e obrigatorio")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter 11 digitos")
        String cpf,

        String phone,

        @Email(message = "E-mail invalido")
        String email
) {
}
