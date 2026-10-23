package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.clients.SmartCareClient;
import br.com.fiap.petjourney.dtos.request.SmartCareAnalysisRequest;
import br.com.fiap.petjourney.dtos.response.SmartCareAnalysisResponse;
import br.com.fiap.petjourney.dtos.response.SmartCareInsufficientDataPetResponse;
import br.com.fiap.petjourney.dtos.response.SmartCarePetAnalysisResponse;
import br.com.fiap.petjourney.dtos.response.SmartCareRankingResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.exceptions.SmartCareDataUnavailableException;
import br.com.fiap.petjourney.models.MedicalRecord;
import br.com.fiap.petjourney.models.Pet;
import br.com.fiap.petjourney.models.enums.AppointmentStatus;
import br.com.fiap.petjourney.models.enums.MedicationStatus;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.AppointmentRepository;
import br.com.fiap.petjourney.repositories.MedicalRecordRepository;
import br.com.fiap.petjourney.repositories.MedicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartCareService {

    private static final List<MedicationStatus> TREATMENT_STATUSES = List.of(
            MedicationStatus.ATIVO,
            MedicationStatus.FINALIZADO
    );

    private final AuthenticatedUserService authenticatedUser;
    private final PetService petService;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicationRepository medicationRepository;
    private final SmartCareClient smartCareClient;

    public SmartCareAnalysisResponse analyze(Long petId) {
        assertAuthorizedRole();
        Pet pet = petService.findAccessiblePet(petId);
        return analyzePet(pet, LocalDateTime.now());
    }

    public SmartCareRankingResponse ranking() {
        assertAuthorizedRole();
        List<Pet> pets = petService.findAccessibleClinicalPets();
        LocalDateTime now = LocalDateTime.now();
        List<SmartCarePetAnalysisResponse> analyses = new ArrayList<>();
        List<SmartCareInsufficientDataPetResponse> insufficientDataPets = new ArrayList<>();

        for (Pet pet : pets) {
            try {
                SmartCareAnalysisResponse analysis = analyzePet(pet, now);
                analyses.add(SmartCarePetAnalysisResponse.from(pet.getName(), analysis));
            } catch (SmartCareDataUnavailableException exception) {
                insufficientDataPets.add(new SmartCareInsufficientDataPetResponse(pet.getId(), pet.getName()));
            }
        }

        analyses.sort(
                Comparator.comparingInt(SmartCarePetAnalysisResponse::score)
                        .reversed()
                        .thenComparing(SmartCarePetAnalysisResponse::petName)
        );

        return new SmartCareRankingResponse(List.copyOf(analyses), List.copyOf(insufficientDataPets));
    }

    private SmartCareAnalysisResponse analyzePet(Pet pet, LocalDateTime now) {
        MedicalRecord latestRecord = medicalRecordRepository
                .findFirstByPetIdAndRegistrationDateLessThanEqualOrderByRegistrationDateDescIdDesc(pet.getId(), now)
                .orElseThrow(() -> new SmartCareDataUnavailableException(
                        "Pet nao possui atendimento clinico registrado para analise SmartCare"
                ));

        boolean hasNextAppointment = appointmentRepository.existsValidUpcomingForPet(
                pet.getId(),
                now,
                AppointmentStatus.PENDENTE
        );
        boolean hasRegisteredTreatment = hasRegisteredTreatment(pet.getId(), latestRecord);

        var request = new SmartCareAnalysisRequest(
                pet.getId(),
                latestRecord.getRegistrationDate().toLocalDate(),
                hasNextAppointment,
                hasRegisteredTreatment
        );

        log.info(
                "SmartCare analysis petId={} ultimoAtendimento={} possuiProximoAgendamento={} possuiTratamentoRegistrado={}",
                request.petId(),
                request.dataUltimoAtendimento(),
                request.possuiProximoAgendamento(),
                request.possuiTratamentoRegistrado()
        );

        return smartCareClient.analyze(request);
    }

    private void assertAuthorizedRole() {
        UserRole role = authenticatedUser.role();
        if (role != UserRole.ADMIN_CLINICA && role != UserRole.VETERINARIO) {
            throw new ForbiddenOperationException("Perfil sem permissao para acessar o SmartCare");
        }
    }

    private boolean hasRegisteredTreatment(Long petId, MedicalRecord latestRecord) {
        return medicationRepository.existsByPetIdAndStatusIn(petId, TREATMENT_STATUSES)
                || StringUtils.hasText(latestRecord.getConduct())
                || StringUtils.hasText(latestRecord.getPrescriptionNotes());
    }
}
