package br.com.fiap.petjourney.config;

import br.com.fiap.petjourney.repositories.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ActiveAccountTokenValidatorTest {
    private final UserAccountRepository repository = mock(UserAccountRepository.class);
    private final ActiveAccountTokenValidator validator = new ActiveAccountTokenValidator(repository);

    @ParameterizedTest
    @ValueSource(strings = {"tutor@example.com", "vet@example.com", "admin@example.com"})
    void rejectsExistingTokenAfterAccountIsDeactivated(String username) {
        Jwt token = Jwt.withTokenValue("test").header("alg", "RS256").subject(username).build();
        when(repository.existsByUsernameAndActiveTrue(username)).thenReturn(true, false);

        assertFalse(validator.validate(token).hasErrors());
        assertTrue(validator.validate(token).hasErrors());
        verify(repository, times(2)).existsByUsernameAndActiveTrue(username);
    }

    @Test
    void rejectsMissingAccount() {
        Jwt token = Jwt.withTokenValue("test").header("alg", "RS256").subject("missing").build();
        assertTrue(validator.validate(token).hasErrors());
    }

    @Test
    void rejectsTokenWithoutSubject() {
        Jwt token = Jwt.withTokenValue("test").header("alg", "RS256").claim("role", "TUTOR").build();
        assertTrue(validator.validate(token).hasErrors());
        verifyNoInteractions(repository);
    }
}
