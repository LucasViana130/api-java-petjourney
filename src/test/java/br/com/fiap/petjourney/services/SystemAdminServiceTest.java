package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.request.CreateClinicAdminRequest;
import br.com.fiap.petjourney.dtos.response.UserAccountResponse;
import br.com.fiap.petjourney.exceptions.ConflictOperationException;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.models.Clinic;
import br.com.fiap.petjourney.models.UserAccount;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.ClinicRepository;
import br.com.fiap.petjourney.repositories.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAdminServiceTest {

    @Mock
    private ClinicRepository clinicRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuthenticatedUserService authenticatedUser;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private SystemAdminService service;

    @BeforeEach
    void authenticateSystemAdmin() {
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_SISTEMA);
    }

    @Test
    void shouldCreateAdminForClinicWithoutAdmin() {
        Clinic clinic = Clinic.builder().id(1L).name("Pet Feliz").build();
        CreateClinicAdminRequest request = new CreateClinicAdminRequest(" Admin@PetFeliz.com ", "123456");
        allowAdminCreation(clinic, "admin@petfeliz.com");

        UserAccountResponse response = service.createClinicAdmin(1L, request);

        assertEquals("admin@petfeliz.com", response.username());
        assertEquals(UserRole.ADMIN_CLINICA, response.role());
        assertEquals(1L, response.clinicId());
    }

    @Test
    void shouldRejectSecondAdminForSameClinic() {
        Clinic clinic = Clinic.builder().id(1L).name("Pet Feliz").build();
        when(clinicRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clinic));
        when(userAccountRepository.existsByClinicIdAndRole(1L, UserRole.ADMIN_CLINICA)).thenReturn(true);

        ConflictOperationException exception = assertThrows(
                ConflictOperationException.class,
                () -> service.createClinicAdmin(
                        1L,
                        new CreateClinicAdminRequest("outro@petfeliz.com", "123456")
                )
        );

        assertEquals("Esta clínica já possui um administrador.", exception.getMessage());
        verify(userAccountRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void shouldCreateAdminForAnotherClinicWithoutAdmin() {
        Clinic clinic = Clinic.builder().id(2L).name("Saúde Animal").build();
        allowAdminCreation(clinic, "admin@saudeanimal.com");

        UserAccountResponse response = service.createClinicAdmin(
                2L,
                new CreateClinicAdminRequest("admin@saudeanimal.com", "123456")
        );

        assertEquals(2L, response.clinicId());
        assertEquals(UserRole.ADMIN_CLINICA, response.role());
    }

    @Test
    void shouldKeepRejectingDuplicateUsername() {
        Clinic clinic = Clinic.builder().id(3L).name("Nova Clínica").build();
        when(clinicRepository.findByIdAndActiveTrue(3L)).thenReturn(Optional.of(clinic));
        when(userAccountRepository.existsByClinicIdAndRole(3L, UserRole.ADMIN_CLINICA)).thenReturn(false);
        when(userAccountRepository.existsByUsername("existente@petjourney.com")).thenReturn(true);

        ForbiddenOperationException exception = assertThrows(
                ForbiddenOperationException.class,
                () -> service.createClinicAdmin(
                        3L,
                        new CreateClinicAdminRequest("existente@petjourney.com", "123456")
                )
        );

        assertEquals("Já existe usuário cadastrado com este e-mail", exception.getMessage());
        verify(userAccountRepository, never()).save(any());
    }

    private void allowAdminCreation(Clinic clinic, String normalizedUsername) {
        when(clinicRepository.findByIdAndActiveTrue(clinic.getId())).thenReturn(Optional.of(clinic));
        when(userAccountRepository.existsByClinicIdAndRole(clinic.getId(), UserRole.ADMIN_CLINICA))
                .thenReturn(false);
        when(userAccountRepository.existsByUsername(normalizedUsername)).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded-password");
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
    }
}
