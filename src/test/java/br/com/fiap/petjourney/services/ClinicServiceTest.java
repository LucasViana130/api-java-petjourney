package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.response.ClinicResponse;
import br.com.fiap.petjourney.models.Clinic;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.ClinicRepository;
import br.com.fiap.petjourney.repositories.PetRepository;
import br.com.fiap.petjourney.repositories.TutorRepository;
import br.com.fiap.petjourney.repositories.UserAccountRepository;
import br.com.fiap.petjourney.repositories.VeterinarianRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicServiceTest {

    @Mock
    private ClinicRepository repository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private VeterinarianRepository veterinarianRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private AuthenticatedUserService authenticatedUser;

    @InjectMocks
    private ClinicService service;

    @Test
    void shouldReturnHasAdminForClinicsUsingOneBatchQuery() {
        Clinic withAdmin = Clinic.builder().id(1L).name("Pet Feliz").build();
        Clinic withoutAdmin = Clinic.builder().id(2L).name("Nova Clínica").build();
        PageRequest pageable = PageRequest.of(0, 10);
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_SISTEMA);
        when(repository.findByActiveTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(withAdmin, withoutAdmin), pageable, 2));
        when(userAccountRepository.findClinicIdsByRole(
                List.of(1L, 2L),
                UserRole.ADMIN_CLINICA
        )).thenReturn(Set.of(1L));

        Page<ClinicResponse> response = service.findAll(pageable);

        assertTrue(response.getContent().get(0).isHasAdmin());
        assertFalse(response.getContent().get(1).isHasAdmin());
        verify(userAccountRepository).findClinicIdsByRole(
                List.of(1L, 2L),
                UserRole.ADMIN_CLINICA
        );
    }

    @Test
    void shouldReturnHasAdminOnClinicDetail() {
        Clinic clinic = Clinic.builder().id(1L).name("Pet Feliz").build();
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_SISTEMA);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clinic));
        when(userAccountRepository.existsByClinicIdAndRole(1L, UserRole.ADMIN_CLINICA))
                .thenReturn(true);

        ClinicResponse response = service.findById(1L);

        assertTrue(response.isHasAdmin());
    }

    @Test
    void shouldReturnHasAdminFalseOnClinicDetailWithoutAdmin() {
        Clinic clinic = Clinic.builder().id(2L).name("Nova Clínica").build();
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_SISTEMA);
        when(repository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(clinic));
        when(userAccountRepository.existsByClinicIdAndRole(2L, UserRole.ADMIN_CLINICA))
                .thenReturn(false);

        ClinicResponse response = service.findById(2L);

        assertFalse(response.isHasAdmin());
    }
}
