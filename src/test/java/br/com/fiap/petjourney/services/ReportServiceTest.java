package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.response.PetReportItemResponse;
import br.com.fiap.petjourney.dtos.response.PetWeightReportItemResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.models.enums.PetSpecies;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.ReportProcedureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportProcedureRepository repository;

    @Mock
    private AuthenticatedUserService authenticatedUser;

    @InjectMocks
    private ReportService service;

    @Test
    void usesClinicFromAuthenticatedAdmin() {
        List<PetReportItemResponse> expected = List.of(
                new PetReportItemResponse(1L, 2L, 42L, "Lua", "Ana", "Clinica")
        );
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_CLINICA);
        when(authenticatedUser.clinicId()).thenReturn(42L);
        when(repository.findPets(42L)).thenReturn(expected);

        List<PetReportItemResponse> result = service.pets();

        assertSame(expected, result);
        verify(repository).findPets(42L);
    }

    @Test
    void passesOptionalSpeciesWithAuthenticatedClinic() {
        List<PetWeightReportItemResponse> expected = List.of();
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_CLINICA);
        when(authenticatedUser.clinicId()).thenReturn(7L);
        when(repository.findPetWeights(7L, PetSpecies.GATO)).thenReturn(expected);

        List<PetWeightReportItemResponse> result = service.petWeights(PetSpecies.GATO);

        assertSame(expected, result);
        verify(repository).findPetWeights(7L, PetSpecies.GATO);
    }

    @Test
    void blocksProfilesOtherThanClinicAdmin() {
        when(authenticatedUser.role()).thenReturn(UserRole.VETERINARIO);

        assertThrows(ForbiddenOperationException.class, service::pets);

        verifyNoInteractions(repository);
    }

    @Test
    void blocksAdminWithoutClinicClaim() {
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_CLINICA);
        when(authenticatedUser.clinicId()).thenReturn(null);

        assertThrows(ForbiddenOperationException.class, service::pets);

        verifyNoInteractions(repository);
    }
}
