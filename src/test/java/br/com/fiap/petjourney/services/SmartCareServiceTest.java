package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.clients.SmartCareClient;
import br.com.fiap.petjourney.dtos.request.SmartCareAnalysisRequest;
import br.com.fiap.petjourney.dtos.response.SmartCareAnalysisResponse;
import br.com.fiap.petjourney.dtos.response.SmartCareRankingResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.exceptions.SmartCareDataUnavailableException;
import br.com.fiap.petjourney.exceptions.SmartCareUnavailableException;
import br.com.fiap.petjourney.models.MedicalRecord;
import br.com.fiap.petjourney.models.Pet;
import br.com.fiap.petjourney.models.enums.AppointmentStatus;
import br.com.fiap.petjourney.models.enums.MedicationStatus;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.AppointmentRepository;
import br.com.fiap.petjourney.repositories.MedicalRecordRepository;
import br.com.fiap.petjourney.repositories.MedicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartCareServiceTest {

    @Mock
    private AuthenticatedUserService authenticatedUser;
    @Mock
    private PetService petService;
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private SmartCareClient smartCareClient;

    @InjectMocks
    private SmartCareService service;

    @Test
    void shouldConsolidateRealFactsAndCallSmartCare() {
        Pet pet = Pet.builder().id(1L).build();
        MedicalRecord latestRecord = MedicalRecord.builder()
                .id(10L)
                .registrationDate(LocalDateTime.of(2025, 8, 6, 14, 30))
                .conduct("Tratamento clinico registrado")
                .build();
        SmartCareAnalysisResponse expectedResponse = analysisFor(1L, 50);

        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_CLINICA);
        when(petService.findAccessiblePet(1L)).thenReturn(pet);
        when(medicalRecordRepository
                .findFirstByPetIdAndRegistrationDateLessThanEqualOrderByRegistrationDateDescIdDesc(eq(1L), any()))
                .thenReturn(Optional.of(latestRecord));
        when(appointmentRepository.existsValidUpcomingForPet(
                eq(1L), any(), eq(AppointmentStatus.PENDENTE)))
                .thenReturn(true);
        when(medicationRepository.existsByPetIdAndStatusIn(eq(1L), any()))
                .thenReturn(false);
        when(smartCareClient.analyze(any())).thenReturn(expectedResponse);

        SmartCareAnalysisResponse response = service.analyze(1L);

        assertEquals(expectedResponse, response);
        ArgumentCaptor<SmartCareAnalysisRequest> requestCaptor = ArgumentCaptor.forClass(SmartCareAnalysisRequest.class);
        verify(smartCareClient).analyze(requestCaptor.capture());
        SmartCareAnalysisRequest request = requestCaptor.getValue();
        assertEquals(1L, request.petId());
        assertEquals(latestRecord.getRegistrationDate().toLocalDate(), request.dataUltimoAtendimento());
        assertTrue(request.possuiProximoAgendamento());
        assertTrue(request.possuiTratamentoRegistrado());
        verify(medicationRepository).existsByPetIdAndStatusIn(
                eq(1L),
                eq(List.of(MedicationStatus.ATIVO, MedicationStatus.FINALIZADO))
        );
    }

    @Test
    void shouldUseMedicationAsRegisteredTreatment() {
        preparePetWithRecord(1L, MedicalRecord.builder()
                .id(10L)
                .registrationDate(LocalDateTime.of(2025, 8, 6, 14, 30))
                .build());
        when(medicationRepository.existsByPetIdAndStatusIn(eq(1L), any())).thenReturn(true);
        when(smartCareClient.analyze(any())).thenReturn(analysisFor(1L, 50));

        service.analyze(1L);

        ArgumentCaptor<SmartCareAnalysisRequest> requestCaptor = ArgumentCaptor.forClass(SmartCareAnalysisRequest.class);
        verify(smartCareClient).analyze(requestCaptor.capture());
        assertTrue(requestCaptor.getValue().possuiTratamentoRegistrado());
    }

    @Test
    void shouldNotCallPythonWhenPetHasNoMedicalRecord() {
        Pet pet = Pet.builder().id(2L).build();

        when(authenticatedUser.role()).thenReturn(UserRole.VETERINARIO);
        when(petService.findAccessiblePet(2L)).thenReturn(pet);
        when(medicalRecordRepository
                .findFirstByPetIdAndRegistrationDateLessThanEqualOrderByRegistrationDateDescIdDesc(eq(2L), any()))
                .thenReturn(Optional.empty());

        assertThrows(SmartCareDataUnavailableException.class, () -> service.analyze(2L));
        verify(smartCareClient, never()).analyze(any());
        verify(appointmentRepository, never()).existsValidUpcomingForPet(any(), any(), any());
    }

    @Test
    void shouldRejectTutorBeforeReadingPetData() {
        when(authenticatedUser.role()).thenReturn(UserRole.TUTOR);

        assertThrows(ForbiddenOperationException.class, service::ranking);
        verify(petService, never()).findAccessibleClinicalPets();
        verify(smartCareClient, never()).analyze(any());
    }

    @Test
    void shouldRejectSystemAdminBeforeReadingPetData() {
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_SISTEMA);

        assertThrows(ForbiddenOperationException.class, service::ranking);
        verify(petService, never()).findAccessibleClinicalPets();
        verify(smartCareClient, never()).analyze(any());
    }

    @Test
    void shouldOrderRankingByScoreDescendingAndNameAscendingOnTies() {
        Pet rex = Pet.builder().id(1L).name("Rex").build();
        Pet bella = Pet.builder().id(2L).name("Bella").build();
        Pet apollo = Pet.builder().id(3L).name("Apollo").build();

        when(authenticatedUser.role()).thenReturn(UserRole.VETERINARIO);
        when(petService.findAccessibleClinicalPets()).thenReturn(List.of(rex, bella, apollo));
        when(medicalRecordRepository
                .findFirstByPetIdAndRegistrationDateLessThanEqualOrderByRegistrationDateDescIdDesc(anyLong(), any()))
                .thenAnswer(invocation -> Optional.of(recordFor(invocation.getArgument(0))));
        when(appointmentRepository.existsValidUpcomingForPet(anyLong(), any(), eq(AppointmentStatus.PENDENTE)))
                .thenReturn(false);
        when(medicationRepository.existsByPetIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(smartCareClient.analyze(any())).thenAnswer(invocation -> {
            SmartCareAnalysisRequest request = invocation.getArgument(0);
            int score = request.petId().equals(1L) ? 50 : 80;
            return analysisFor(request.petId(), score);
        });

        SmartCareRankingResponse response = service.ranking();

        assertEquals(List.of(3L, 2L, 1L), response.analises().stream().map(item -> item.petId()).toList());
        assertTrue(response.semDadosSuficientes().isEmpty());
        verify(smartCareClient, times(3)).analyze(any());
    }

    @Test
    void shouldSeparatePetWithoutMedicalRecordWithoutArtificialScore() {
        Pet rex = Pet.builder().id(1L).name("Rex").build();
        Pet mel = Pet.builder().id(2L).name("Mel").build();

        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_CLINICA);
        when(petService.findAccessibleClinicalPets()).thenReturn(List.of(rex, mel));
        when(medicalRecordRepository
                .findFirstByPetIdAndRegistrationDateLessThanEqualOrderByRegistrationDateDescIdDesc(eq(1L), any()))
                .thenReturn(Optional.of(recordFor(1L)));
        when(medicalRecordRepository
                .findFirstByPetIdAndRegistrationDateLessThanEqualOrderByRegistrationDateDescIdDesc(eq(2L), any()))
                .thenReturn(Optional.empty());
        when(appointmentRepository.existsValidUpcomingForPet(eq(1L), any(), eq(AppointmentStatus.PENDENTE)))
                .thenReturn(false);
        when(medicationRepository.existsByPetIdAndStatusIn(eq(1L), any())).thenReturn(false);
        when(smartCareClient.analyze(any())).thenReturn(analysisFor(1L, 50));

        SmartCareRankingResponse response = service.ranking();

        assertEquals(1, response.analises().size());
        assertEquals(1L, response.analises().get(0).petId());
        assertEquals(1, response.semDadosSuficientes().size());
        assertEquals(2L, response.semDadosSuficientes().get(0).petId());
        assertEquals("Mel", response.semDadosSuficientes().get(0).petName());
        verify(smartCareClient, times(1)).analyze(any());
    }

    @Test
    void shouldAbortEntireRankingWhenSmartCareIsUnavailable() {
        Pet rex = Pet.builder().id(1L).name("Rex").build();
        Pet thor = Pet.builder().id(2L).name("Thor").build();

        when(authenticatedUser.role()).thenReturn(UserRole.VETERINARIO);
        when(petService.findAccessibleClinicalPets()).thenReturn(List.of(rex, thor));
        when(medicalRecordRepository
                .findFirstByPetIdAndRegistrationDateLessThanEqualOrderByRegistrationDateDescIdDesc(anyLong(), any()))
                .thenAnswer(invocation -> Optional.of(recordFor(invocation.getArgument(0))));
        when(appointmentRepository.existsValidUpcomingForPet(anyLong(), any(), eq(AppointmentStatus.PENDENTE)))
                .thenReturn(false);
        when(medicationRepository.existsByPetIdAndStatusIn(anyLong(), any())).thenReturn(false);
        when(smartCareClient.analyze(any()))
                .thenReturn(analysisFor(1L, 50))
                .thenThrow(new SmartCareUnavailableException("Servico SmartCare indisponivel no momento", null));

        SmartCareUnavailableException exception = assertThrows(
                SmartCareUnavailableException.class,
                service::ranking
        );

        assertEquals("Servico SmartCare indisponivel no momento", exception.getMessage());
        verify(smartCareClient, times(2)).analyze(any());
    }

    private void preparePetWithRecord(Long petId, MedicalRecord record) {
        when(authenticatedUser.role()).thenReturn(UserRole.ADMIN_CLINICA);
        when(petService.findAccessiblePet(petId)).thenReturn(Pet.builder().id(petId).build());
        when(medicalRecordRepository
                .findFirstByPetIdAndRegistrationDateLessThanEqualOrderByRegistrationDateDescIdDesc(eq(petId), any()))
                .thenReturn(Optional.of(record));
        when(appointmentRepository.existsValidUpcomingForPet(
                eq(petId), any(), eq(AppointmentStatus.PENDENTE)))
                .thenReturn(false);
    }

    private MedicalRecord recordFor(Long petId) {
        return MedicalRecord.builder()
                .id(petId * 10)
                .registrationDate(LocalDateTime.of(2025, 8, 6, 14, 30))
                .build();
    }

    private SmartCareAnalysisResponse analysisFor(Long petId, int score) {
        return new SmartCareAnalysisResponse(
                petId,
                score,
                score >= 70 ? "ALTA" : "MEDIA",
                List.of("Motivo de teste"),
                "Recomendacao de teste"
        );
    }
}
