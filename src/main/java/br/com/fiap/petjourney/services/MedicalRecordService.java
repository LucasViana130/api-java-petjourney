package br.com.fiap.petjourney.services;

import br.com.fiap.petjourney.dtos.request.MedicalRecordRequest;
import br.com.fiap.petjourney.dtos.response.MedicalRecordResponse;
import br.com.fiap.petjourney.exceptions.ForbiddenOperationException;
import br.com.fiap.petjourney.exceptions.ResourceNotFoundException;
import br.com.fiap.petjourney.models.Appointment;
import br.com.fiap.petjourney.models.MedicalRecord;
import br.com.fiap.petjourney.models.Pet;
import br.com.fiap.petjourney.models.Veterinarian;
import br.com.fiap.petjourney.models.enums.UserRole;
import br.com.fiap.petjourney.repositories.AppointmentRepository;
import br.com.fiap.petjourney.repositories.MedicalRecordRepository;
import br.com.fiap.petjourney.repositories.PetRepository;
import br.com.fiap.petjourney.repositories.VeterinarianRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MedicalRecordService {

    private final MedicalRecordRepository repository;
    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final VeterinarianRepository veterinarianRepository;
    private final AuthenticatedUserService authenticatedUser;

    @Cacheable(
            value = "medicalRecords",
            key = "{@authenticatedUserService.username(), 'pet', #petId, #pageable.toString()}"
    )
    public Page<MedicalRecordResponse> findByPetId(Long petId, Pageable pageable) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.TUTOR) {
            return repository.findByPetIdAndPetTutorId(petId, authenticatedUser.tutorId(), pageable)
                    .map(MedicalRecordResponse::fromEntity);
        }
        if (role == UserRole.ADMIN_CLINICA) {
            return repository.findByPetIdAndVeterinarianClinicId(petId, authenticatedUser.clinicId(), pageable)
                    .map(MedicalRecordResponse::fromEntity);
        }
        if (role == UserRole.VETERINARIO) {
            return repository.findByPetIdAndVeterinarianId(petId, authenticatedUser.veterinarianId(), pageable)
                    .map(MedicalRecordResponse::fromEntity);
        }
        return repository.findByPetId(petId, pageable).map(MedicalRecordResponse::fromEntity);
    }

    @Cacheable(
            value = "medicalRecords",
            key = "{@authenticatedUserService.username(), 'range', #start, #end, #pageable.toString()}"
    )
    public Page<MedicalRecordResponse> findByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.TUTOR) {
            return repository.findByPetTutorIdAndRegistrationDateBetween(authenticatedUser.tutorId(), start, end, pageable)
                    .map(MedicalRecordResponse::fromEntity);
        }
        if (role == UserRole.ADMIN_CLINICA) {
            return repository.findByVeterinarianClinicIdAndRegistrationDateBetween(authenticatedUser.clinicId(), start, end, pageable)
                    .map(MedicalRecordResponse::fromEntity);
        }
        if (role == UserRole.VETERINARIO) {
            return repository.findByVeterinarianIdAndRegistrationDateBetween(authenticatedUser.veterinarianId(), start, end, pageable)
                    .map(MedicalRecordResponse::fromEntity);
        }
        return repository.findByRegistrationDateBetween(start, end, pageable).map(MedicalRecordResponse::fromEntity);
    }

    @Cacheable(value = "medicalRecords", key = "{@authenticatedUserService.username(), 'id', #id}")
    public MedicalRecordResponse findById(Long id) {
        return MedicalRecordResponse.fromEntity(findAccessibleMedicalRecord(id));
    }

    @Cacheable(value = "medicalRecords", key = "{@authenticatedUserService.username(), 'appointment', #appointmentId}")
    public MedicalRecordResponse findByAppointmentId(Long appointmentId) {
        return MedicalRecordResponse.fromEntity(findAccessibleMedicalRecordByAppointmentId(appointmentId));
    }

    public MedicalRecord findAccessibleMedicalRecordByAppointmentId(Long appointmentId) {
        MedicalRecord record = repository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Relatório da consulta não encontrado"));
        assertReadAccess(record);
        return record;
    }

    public boolean existsByAppointmentId(Long appointmentId) {
        return repository.existsByAppointmentId(appointmentId);
    }

    @CacheEvict(value = "medicalRecords", allEntries = true)
    public MedicalRecordResponse create(MedicalRecordRequest request) {
        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado"));

        Veterinarian veterinarian = veterinarianRepository.findById(request.veterinarianId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado"));

        assertWriteAccess(pet, veterinarian);
        MedicalRecord medicalRecord = new MedicalRecord(request, pet, veterinarian);
        medicalRecord.setAppointment(resolveAppointmentForRequest(request.appointmentId()));
        return MedicalRecordResponse.fromEntity(repository.save(medicalRecord));
    }

    @CacheEvict(value = "medicalRecords", allEntries = true)
    public MedicalRecordResponse update(Long id, MedicalRecordRequest request) {
        MedicalRecord medicalRecord = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado"));

        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado"));

        Veterinarian veterinarian = veterinarianRepository.findById(request.veterinarianId())
                .orElseThrow(() -> new ResourceNotFoundException("Veterinário não encontrado"));

        assertWriteAccess(pet, veterinarian);
        medicalRecord.updateFrom(request, pet, veterinarian);
        medicalRecord.setAppointment(resolveAppointmentForRequest(request.appointmentId()));

        return MedicalRecordResponse.fromEntity(repository.save(medicalRecord));
    }

    @CacheEvict(value = "medicalRecords", allEntries = true)
    public void delete(Long id) {
        MedicalRecord medicalRecord = findAccessibleMedicalRecord(id);
        repository.delete(medicalRecord);
    }

    @CacheEvict(value = "medicalRecords", allEntries = true)
    public MedicalRecord saveFromConsultation(MedicalRecord medicalRecord) {
        return repository.save(medicalRecord);
    }

    public byte[] generatePdfByAppointmentId(Long appointmentId, ConsultationReportPdfService pdfService) {
        MedicalRecord record = findAccessibleMedicalRecordByAppointmentId(appointmentId);
        return pdfService.generate(record);
    }

    private MedicalRecord findAccessibleMedicalRecord(Long id) {
        MedicalRecord record = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prontuário não encontrado"));

        assertReadAccess(record);

        return record;
    }

    private void assertReadAccess(MedicalRecord record) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.TUTOR && !record.getPet().getTutor().getId().equals(authenticatedUser.tutorId())) {
            throw new ForbiddenOperationException("Tutor não pode acessar prontuário de outro tutor");
        }
        if (role == UserRole.ADMIN_CLINICA && !record.getVeterinarian().getClinic().getId().equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Administrador não pode acessar prontuário de outra clínica");
        }
        if (role == UserRole.VETERINARIO && !record.getVeterinarian().getId().equals(authenticatedUser.veterinarianId())) {
            throw new ForbiddenOperationException("Veterinário não pode acessar prontuário de outro veterinário");
        }

    }

    private void assertWriteAccess(Pet pet, Veterinarian veterinarian) {
        UserRole role = authenticatedUser.role();
        if (role == UserRole.TUTOR) {
            throw new ForbiddenOperationException("Tutor não pode registrar prontuário clínico");
        }
        if (veterinarian.getClinic() == null
                || pet.getTutor().getClinic() == null
                || !veterinarian.getClinic().getId().equals(pet.getTutor().getClinic().getId())) {
            throw new ForbiddenOperationException("Pet não pertence à carteira de clientes da clínica");
        }
        if (role == UserRole.ADMIN_CLINICA && !veterinarian.getClinic().getId().equals(authenticatedUser.clinicId())) {
            throw new ForbiddenOperationException("Administrador não pode registrar prontuário em outra clínica");
        }
        if (role == UserRole.VETERINARIO && !veterinarian.getId().equals(authenticatedUser.veterinarianId())) {
            throw new ForbiddenOperationException("Veterinário não pode registrar prontuário para outro veterinário");
        }
    }

    private Appointment resolveAppointmentForRequest(Long appointmentId) {
        if (appointmentId == null) {
            return null;
        }
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado"));
    }
}
