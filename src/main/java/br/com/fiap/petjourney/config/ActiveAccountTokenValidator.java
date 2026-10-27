package br.com.fiap.petjourney.config;

import br.com.fiap.petjourney.repositories.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class ActiveAccountTokenValidator implements OAuth2TokenValidator<Jwt> {

    private final UserAccountRepository userAccountRepository;

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (StringUtils.hasText(token.getSubject())
                && userAccountRepository.existsByUsernameAndActiveTrue(token.getSubject())) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Sessao invalida ou conta inativa", null)
        );
    }
}
