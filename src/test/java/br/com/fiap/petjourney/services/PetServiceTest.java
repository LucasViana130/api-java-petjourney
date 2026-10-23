package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.models.Pet;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.PetRepository;
import br.com.fiap.petjourney.repositories.TutorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository repository;
    @Mock
    private TutorRepository tutorRepository;
    @Mock
    private AuthenticatedUserService authenticatedUser;

    @InjectMocks
    private PetService service;

    @Test
    void shouldLoadRankingPetsOnlyFromAuthenticatedClinic() {
        Pet pet = Pet.builder().id(7L).name("Rex").build();
        when(authenticatedUser.role()).thenReturn(UserRole.VETERINARIO);
        when(authenticatedUser.clinicId()).thenReturn(42L);
        when(repository.findPatientsByClinicId(42L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(pet)));

        List<Pet> result = service.findAccessibleClinicalPets();

        assertEquals(List.of(pet), result);
        verify(repository).findPatientsByClinicId(42L, Pageable.unpaged());
    }

    @Test
    void shouldRejectTutorWithoutQueryingClinicalPets() {
        when(authenticatedUser.role()).thenReturn(UserRole.TUTOR);

        assertThrows(ForbiddenOperationException.class, service::findAccessibleClinicalPets);

        verifyNoInteractions(repository);
    }
}
